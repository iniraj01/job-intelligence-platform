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
}
