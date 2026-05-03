package com.example.demo.service;

import com.example.demo.model.Job;
import com.example.demo.model.Reference;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class RealJobService {

    private List<Job> realJobs = new ArrayList<>();

    @PostConstruct
    public void init() {
        // Fetch jobs on startup
        refreshJobs();
    }

    @Scheduled(fixedRate = 3600000) // 1 hour
    public void refreshJobs() {
        try {
            System.out.println("Jobs refreshed at: " + LocalDateTime.now());
            
            java.io.File scriptFile = new java.io.File("job_fetcher.py");
            if (!scriptFile.exists()) {
                System.out.println("job_fetcher.py not found. Skipping job fetch.");
                return;
            }
            
            ProcessBuilder pb = new ProcessBuilder("python3", "job_fetcher.py");
            pb.redirectError(ProcessBuilder.Redirect.INHERIT); // Sends stderr straight to console, prevents deadlock
            Process process = pb.start();
            
            // Read stdout (JSON)
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
                List<Map<String, String>> fetchedJobs = mapper.readValue(output.toString(), new TypeReference<List<Map<String, String>>>() {});
                
                List<Job> parsedJobs = new ArrayList<>();
                for (Map<String, String> jobData : fetchedJobs) {
                    Job job = new Job();
                    job.setId(UUID.randomUUID().toString());
                    job.setTitle(jobData.get("title"));
                    job.setCompany(jobData.get("company"));
                    job.setLink(jobData.get("link"));
                    
                    System.out.println("REAL LINK: " + job.getLink());
                    
                    // Auto-tag skills and experience based on title
                    job.setRequiredSkills(autoTagSkills(job.getTitle()));
                    job.setMinExperience(autoTagExperience(job.getTitle()));
                    
                    parsedJobs.add(job);
                }
                
                if (!parsedJobs.isEmpty()) {
                    this.realJobs = parsedJobs;
                }
            } else {
                System.err.println("job_fetcher.py failed: " + output.toString());
            }
            
        } catch (Exception e) {
            System.err.println("Error fetching real jobs: " + e.getMessage());
        }
    }
    
    public List<Job> getRealJobs() {
        return new ArrayList<>(this.realJobs);
    }
    
    public List<Job> fetchDynamicJobs(List<String> skills) {
        if (skills == null || skills.isEmpty()) {
            return getRealJobs();
        }
        
        try {
            java.io.File scriptFile = new java.io.File("job_fetcher.py");
            if (!scriptFile.exists()) {
                System.out.println("job_fetcher.py not found. Returning cached jobs.");
                return getRealJobs();
            }
            
            // Build query using up to 3 skills dynamically
            List<String> querySkills = skills.size() > 3 ? skills.subList(0, 3) : skills;
            String query = String.join(" OR ", querySkills);
            
            ProcessBuilder pb = new ProcessBuilder("python3", "job_fetcher.py", query);
            pb.redirectError(ProcessBuilder.Redirect.INHERIT);
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
                List<Map<String, String>> fetchedJobs = mapper.readValue(output.toString(), new TypeReference<List<Map<String, String>>>() {});
                
                List<Job> parsedJobs = new ArrayList<>();
                for (Map<String, String> jobData : fetchedJobs) {
                    Job job = new Job();
                    job.setId(UUID.randomUUID().toString());
                    job.setTitle(jobData.get("title"));
                    job.setCompany(jobData.get("company"));
                    job.setLink(jobData.get("link"));
                    
                    job.setRequiredSkills(autoTagSkills(job.getTitle()));
                    job.setMinExperience(autoTagExperience(job.getTitle()));
                    
                    parsedJobs.add(job);
                }
                
                if (!parsedJobs.isEmpty()) {
                    return parsedJobs;
                }
            }
            
        } catch (Exception e) {
            System.err.println("Error fetching dynamic jobs: " + e.getMessage());
        }
        
        // Fallback to cached jobs
        return getRealJobs();
    }
    
    private List<String> autoTagSkills(String title) {
        List<String> skills = new ArrayList<>();
        String t = title.toLowerCase();
        
        if (t.contains("java") || t.contains("spring")) {
            skills.add("Java");
            skills.add("Spring Boot");
        }
        if (t.contains("python") || t.contains("data")) {
            skills.add("Python");
            skills.add("SQL");
        }
        if (t.contains("frontend") || t.contains("react")) {
            skills.add("React");
            skills.add("JavaScript");
        }
        if (t.contains("backend")) {
            skills.add("Node.js");
            skills.add("SQL");
        }
        if (t.contains("cloud") || t.contains("devops")) {
            skills.add("AWS");
            skills.add("Docker");
            skills.add("Linux");
        }
        if (t.contains("cybersecurity") || t.contains("security")) {
            skills.add("Cybersecurity");
            skills.add("Network Security");
        }
        if (t.contains("iot")) {
            skills.add("IoT");
            skills.add("C++");
            skills.add("Python");
        }
        
        // If no skills matched, just return empty list so it doesn't penalize or bias
        return skills;
    }
    
    private int autoTagExperience(String title) {
        String t = title.toLowerCase();
        if (t.contains("senior") || t.contains("sr") || t.contains("lead")) return 5;
        if (t.contains("mid")) return 3;
        if (t.contains("junior") || t.contains("jr") || t.contains("intern")) return 0;
        return 2; // default
    }
}
