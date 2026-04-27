package com.example.demo.model;

import java.util.List;

public class Job {
    private String id;
    private String title;
    private String company;
    private List<String> requiredSkills;
    private int minExperience;
    private double matchPercentage;
    private String whyMatched;
    private String link;
    private String referenceLink;
    private String referenceMessage;

    public Job() {}

    public Job(String id, String title, String company, List<String> requiredSkills, int minExperience) {
        this.id = id;
        this.title = title;
        this.company = company;
        this.requiredSkills = requiredSkills;
        this.minExperience = minExperience;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getCompany() {
        return company;
    }

    public void setCompany(String company) {
        this.company = company;
    }

    public List<String> getRequiredSkills() {
        return requiredSkills;
    }

    public void setRequiredSkills(List<String> requiredSkills) {
        this.requiredSkills = requiredSkills;
    }

    public int getMinExperience() {
        return minExperience;
    }

    public void setMinExperience(int minExperience) {
        this.minExperience = minExperience;
    }

    public double getMatchPercentage() {
        return matchPercentage;
    }

    public void setMatchPercentage(double matchPercentage) {
        this.matchPercentage = matchPercentage;
    }

    public String getWhyMatched() {
        return whyMatched;
    }

    public void setWhyMatched(String whyMatched) {
        this.whyMatched = whyMatched;
    }

    public String getLink() {
        return link;
    }

    public void setLink(String link) {
        this.link = link;
    }

    public String getReferenceLink() {
        return referenceLink;
    }

    public void setReferenceLink(String referenceLink) {
        this.referenceLink = referenceLink;
    }

    public String getReferenceMessage() {
        return referenceMessage;
    }

    public void setReferenceMessage(String referenceMessage) {
        this.referenceMessage = referenceMessage;
    }
}
