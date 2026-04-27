package com.example.demo.controller;

import com.example.demo.model.Application;
import com.example.demo.repository.ApplicationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@CrossOrigin(origins = "http://localhost:8080")
public class ApplicationController {

    @Autowired
    private ApplicationRepository applicationRepository;

    @PostMapping("/applyJob")
    public ResponseEntity<?> applyJob(@RequestBody Application application) {
        if (application.getUserId() == null || application.getUserId().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "User not logged in"));
        }
        
        application.setAppliedAt(LocalDateTime.now());
        applicationRepository.save(application);
        
        return ResponseEntity.ok(Map.of("message", "Job application tracked successfully"));
    }

    @GetMapping("/myApplications")
    public ResponseEntity<List<Application>> getMyApplications(@RequestParam String userId) {
        List<Application> apps = applicationRepository.findByUserIdOrderByAppliedAtDesc(userId);
        return ResponseEntity.ok(apps);
    }
}
