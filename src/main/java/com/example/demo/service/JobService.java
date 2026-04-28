package com.example.demo.service;

import com.example.demo.model.Job;
import com.example.demo.model.Reference;
import com.example.demo.model.ResumeData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class JobService {

    @Autowired
    private RealJobService realJobService;

    public List<Job> matchJobs(ResumeData resumeData) {
        if (resumeData == null) {
            return new ArrayList<>();
        }

        List<Job> matchedJobs = new ArrayList<>();
        List<String> userSkills = resumeData.getSkills() != null ? 
                resumeData.getSkills().stream().map(String::toUpperCase).collect(Collectors.toList()) : new ArrayList<>();
        int userExp = resumeData.getExperience_years();
        List<String> userRoles = resumeData.getRoles() != null ? 
                resumeData.getRoles().stream().map(String::toLowerCase).collect(Collectors.toList()) : new ArrayList<>();

        // Use real jobs fetched by Python script
        List<Job> jobsToMatch = realJobService.getRealJobs();

        for (Job job : jobsToMatch) {
            // 1. Calculate skills match (50%)
            long matchCount = 0;
            if (job.getRequiredSkills() != null && !job.getRequiredSkills().isEmpty()) {
                for (String reqSkill : job.getRequiredSkills()) {
                    if (userSkills.contains(reqSkill.toUpperCase())) {
                        matchCount++;
                    }
                }
            }
            double skillScore = job.getRequiredSkills().isEmpty() ? 100.0 : ((double) matchCount / job.getRequiredSkills().size()) * 100.0;
                    
            // 2. Calculate experience match (30%)
            double expScore = 100.0;
            if (userExp < job.getMinExperience()) {
                expScore = ((double) userExp / Math.max(1, job.getMinExperience())) * 100.0;
            }

            // 3. Calculate role match (20%)
            double roleScore = 0.0;
            String jobTitleLower = job.getTitle().toLowerCase();
            for (String role : userRoles) {
                if (jobTitleLower.contains(role) || role.contains(jobTitleLower)) {
                    roleScore = 100.0;
                    break;
                }
            }
            if (roleScore == 0.0) {
                roleScore = userRoles.isEmpty() ? 50.0 : 0.0;
            }

            // Overall match calculation
            double baseMatch = (skillScore * 0.50) + (expScore * 0.30) + (roleScore * 0.20);
            
            // Add slight dynamic variance (-3 to +7) to simulate hidden job requirements and ensure different scores
            int variance = (int) (Math.random() * 11) - 3;
            double overallMatch = Math.min(100.0, Math.max(0.0, baseMatch + variance));
            
            int finalScore = (int) Math.round(overallMatch);
            
            if (finalScore > 15) { // Return decent matches
                Job matchedJob = new Job(job.getId(), job.getTitle(), job.getCompany(), job.getRequiredSkills(), job.getMinExperience());
                matchedJob.setLink(job.getLink()); // Ensure link is mapped
                matchedJob.setMatchPercentage(finalScore);
                
                System.out.println("Match Score: " + finalScore);
                
                String why = String.format("Matched %d skills (50%%). Experience %s (30%%). Role similarity is %s (20%%).", 
                        matchCount, 
                        (userExp >= job.getMinExperience() ? "fits" : "is lower"),
                        (roleScore > 50 ? "high" : "low")
                );
                matchedJob.setWhyMatched(why);
                
                // Add LinkedIn search link for networking
                try {
                    String query = java.net.URLEncoder.encode(job.getTitle() + " " + job.getCompany(), "UTF-8");
                    matchedJob.setReferenceLink("https://www.linkedin.com/search/results/people/?keywords=" + query);
                } catch (Exception e) {
                    matchedJob.setReferenceLink("https://www.linkedin.com/search/results/people/");
                }
                matchedJob.setReferenceMessage("Find employees who can refer you");
                
                matchedJobs.add(matchedJob);
            }
        }
        
        // Sort by match percentage descending
        matchedJobs.sort((j1, j2) -> Double.compare(j2.getMatchPercentage(), j1.getMatchPercentage()));
        
        return matchedJobs;
    }

    public List<Job> getSampleJobs() {
        return realJobService.getRealJobs();
    }
}
