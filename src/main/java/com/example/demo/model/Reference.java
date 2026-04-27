package com.example.demo.model;

public class Reference {
    private String name;
    private String role;
    private String company;
    private String linkedin;

    public Reference() {}

    public Reference(String name, String role, String company, String linkedin) {
        this.name = name;
        this.role = role;
        this.company = company;
        this.linkedin = linkedin;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getCompany() {
        return company;
    }

    public void setCompany(String company) {
        this.company = company;
    }

    public String getLinkedin() {
        return linkedin;
    }

    public void setLinkedin(String linkedin) {
        this.linkedin = linkedin;
    }
}
