package com.example.demo.controller;

import com.example.demo.service.FileUploadService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.example.demo.model.ResumeData;

@RestController
@RequestMapping("/api")
public class FileUploadController {

    @Autowired
    private FileUploadService fileUploadService;

    @PostMapping(value = "/uploadResume", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> uploadResume(@RequestParam("file") MultipartFile file) {
        try {
            // Validate file
            if (file.isEmpty()) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("fileName", null);
                errorResponse.put("message", "File is empty");
                errorResponse.put("fileSize", 0L);
                return ResponseEntity.badRequest().body(errorResponse);
            }

            // Check if file is PDF
            String fileName = file.getOriginalFilename();
            if (fileName == null || !fileName.toLowerCase().endsWith(".pdf")) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("fileName", null);
                errorResponse.put("message", "Only PDF files are allowed");
                errorResponse.put("fileSize", 0L);
                return ResponseEntity.badRequest().body(errorResponse);
            }

            // Upload file
            String savedFileName = fileUploadService.uploadFile(file);
            long fileSize = fileUploadService.getFileSize(file);
            
            // Get absolute path
            String absolutePath = fileUploadService.getFilePath(savedFileName);

            // Call Python script
            ResumeData parsedData = new ResumeData();
            try {
                ProcessBuilder pb = new ProcessBuilder("python3", "extract_skills.py", absolutePath);
                pb.redirectErrorStream(true);
                Process process = pb.start();
                
                StringBuilder output = new StringBuilder();
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        output.append(line);
                    }
                }
                
                int exitCode = process.waitFor();
                if (exitCode == 0) {
                    ObjectMapper mapper = new ObjectMapper();
                    try {
                        parsedData = mapper.readValue(output.toString(), ResumeData.class);
                        if (parsedData.getRaw_text() != null) {
                            int javaCalculatedExp = calculateExperience(parsedData.getRaw_text());
                            parsedData.setExperience_years(javaCalculatedExp);
                        }
                    } catch (Exception e) {
                        System.err.println("Failed to parse Python output: " + output.toString());
                        e.printStackTrace();
                    }
                } else {
                    System.err.println("Python script failed with exit code: " + exitCode);
                    System.err.println("Output: " + output.toString());
                }
            } catch (Exception e) {
                System.err.println("Error running Python script: " + e.getMessage());
            }

            Map<String, Object> response = new HashMap<>();
            response.put("fileName", savedFileName);
            response.put("message", "File uploaded successfully");
            response.put("fileSize", fileSize);
            response.put("resumeData", parsedData);

            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (IOException e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("fileName", null);
            errorResponse.put("message", "File upload failed: " + e.getMessage());
            errorResponse.put("fileSize", 0L);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    private int calculateExperience(String text) {
        if (text == null || text.trim().isEmpty()) return 0;
        
        // 1. Direct experience check
        Pattern directPattern = Pattern.compile("(\\d+)\\s*(years|yrs|year)", Pattern.CASE_INSENSITIVE);
        Matcher directMatcher = directPattern.matcher(text);
        int maxDirectExp = 0;
        while (directMatcher.find()) {
            try {
                int exp = Integer.parseInt(directMatcher.group(1));
                if (exp > maxDirectExp && exp < 50) { // arbitrary sanity check
                    maxDirectExp = exp;
                }
            } catch (NumberFormatException ignored) {}
        }
        
        if (maxDirectExp > 0) {
            return maxDirectExp;
        }
        
        // 2. Date range check
        // Patterns like "Jan 2022 - June 2022" or "Jan 2022 to June 2022" or "Jan 2022 – June 2022"
        Pattern rangePattern = Pattern.compile("([A-Za-z]{3,9}\\s+\\d{4})\\s*(?:-|–|to)\\s*([A-Za-z]{3,9}\\s+\\d{4}|Present|Current)", Pattern.CASE_INSENSITIVE);
        Matcher rangeMatcher = rangePattern.matcher(text);
        
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM yyyy", Locale.ENGLISH);
        DateTimeFormatter fullMonthFormatter = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH);
        
        long totalMonths = 0;
        
        while (rangeMatcher.find()) {
            String startStr = rangeMatcher.group(1).trim();
            String endStr = rangeMatcher.group(2).trim();
            
            try {
                YearMonth start = parseDate(startStr, formatter, fullMonthFormatter);
                YearMonth end;
                if (endStr.equalsIgnoreCase("Present") || endStr.equalsIgnoreCase("Current")) {
                    end = YearMonth.now();
                } else {
                    end = parseDate(endStr, formatter, fullMonthFormatter);
                }
                
                if (start != null && end != null && !end.isBefore(start)) {
                    totalMonths += ChronoUnit.MONTHS.between(start, end);
                }
            } catch (Exception e) {
                // Ignore parsing errors for individual ranges
            }
        }
        
        return (int) Math.round(totalMonths / 12.0);
    }

    private YearMonth parseDate(String dateStr, DateTimeFormatter shortFmt, DateTimeFormatter longFmt) {
        // Format string properly for parser e.g., "Jan 2022"
        String[] parts = dateStr.split("\\s+");
        if (parts.length == 2) {
            String month = parts[0];
            String year = parts[1];
            // ensure month is capitalized properly for matching
            month = month.substring(0, 1).toUpperCase() + month.substring(1).toLowerCase();
            String formatted = month + " " + year;
            try {
                return YearMonth.parse(formatted, shortFmt);
            } catch (DateTimeParseException e) {
                try {
                    return YearMonth.parse(formatted, longFmt);
                } catch (DateTimeParseException e2) {
                    return null;
                }
            }
        }
        return null;
    }
}
