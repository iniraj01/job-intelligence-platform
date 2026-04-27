package com.example.demo.controller;

import com.example.demo.model.Job;
import com.example.demo.model.ResumeData;
import com.example.demo.service.JobService;
import com.example.demo.service.RealJobService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/jobs")
@CrossOrigin(origins = "http://localhost:3000") // From user prompt
public class JobController {

    @Autowired
    private JobService jobService;
    
    @Autowired
    private RealJobService realJobService;

    @PostMapping("/match")
    public List<Job> matchJobs(@RequestBody ResumeData resumeData) {
        return jobService.matchJobs(resumeData);
    }

    @GetMapping("/match/test")
    public String testEndpoint() {
        return "API is working";
    }

    @GetMapping("/real")
    public List<Job> getRealJobs() {
        return realJobService.getRealJobs();
    }

    @GetMapping("/sample")
    public List<Job> getSampleJobs() {
        return jobService.getSampleJobs();
    }
}
