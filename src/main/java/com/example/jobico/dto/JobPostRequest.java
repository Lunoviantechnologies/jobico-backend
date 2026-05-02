package com.example.jobico.dto;

import jakarta.validation.constraints.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class JobPostRequest {

    @NotBlank(message = "Job title is required")
    @Size(max = 150, message = "Job title must not exceed 150 characters")
    private String title;

    /**
     * Accepted values: IT | Non-IT | Blue-collar | Freelancer
     */
    @NotBlank(message = "Category is required")
    private String category;

    /**
     * Accepted values: Full-time | Part-time | Contract | Internship | Freelance
     */
    private String jobType = "Full-time";

    /**
     * Accepted values: Remote | On-site | Hybrid
     */
    private String workType = "Remote";

    @NotBlank(message = "Location is required")
    @Size(max = 100, message = "Location must not exceed 100 characters")
    private String location;

    @NotBlank(message = "Experience required field is required")
    @Size(max = 50, message = "Experience string must not exceed 50 characters")
    private String experienceRequired;

    @Min(value = 1, message = "At least 1 opening is required")
    private int numberOfOpenings = 1;

    // ─── Compensation 

    /** Nullable — salary is optional / not disclosed. */
    @DecimalMin(value = "0.0", inclusive = false, message = "Minimum salary must be positive")
    private Double salaryMin;

    @DecimalMin(value = "0.0", inclusive = false, message = "Maximum salary must be positive")
    private Double salaryMax;

    // ─── Application Window 

    /** Nullable — no deadline means rolling applications. */
    private LocalDate applicationDeadline;

    //  Tech Stack / Skills 

    /**
     * Flat list of skill/tech tags, e.g. ["React", "TypeScript", "Node.js"].
     * Frontend sends them as a single merged array regardless of which
     * category group (Frontend / Backend / Mobile / etc.) was used.
     */
    private List<String> techStack = new ArrayList<>();

    // ─── Job Details ──────────────────────────────────────────────────────────

    @NotBlank(message = "Job description is required")
    private String description;

    private String responsibilities;

    private String requirementsAndQualifications;

    // Getters & Setters

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getJobType() { return jobType; }
    public void setJobType(String jobType) { this.jobType = jobType; }

    public String getWorkType() { return workType; }
    public void setWorkType(String workType) { this.workType = workType; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public String getExperienceRequired() { return experienceRequired; }
    public void setExperienceRequired(String experienceRequired) { this.experienceRequired = experienceRequired; }

    public int getNumberOfOpenings() { return numberOfOpenings; }
    public void setNumberOfOpenings(int numberOfOpenings) { this.numberOfOpenings = numberOfOpenings; }

    public Double getSalaryMin() { return salaryMin; }
    public void setSalaryMin(Double salaryMin) { this.salaryMin = salaryMin; }

    public Double getSalaryMax() { return salaryMax; }
    public void setSalaryMax(Double salaryMax) { this.salaryMax = salaryMax; }

    public LocalDate getApplicationDeadline() { return applicationDeadline; }
    public void setApplicationDeadline(LocalDate applicationDeadline) { this.applicationDeadline = applicationDeadline; }

    public List<String> getTechStack() { return techStack; }
    public void setTechStack(List<String> techStack) { this.techStack = techStack; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getResponsibilities() { return responsibilities; }
    public void setResponsibilities(String responsibilities) { this.responsibilities = responsibilities; }

    public String getRequirementsAndQualifications() { return requirementsAndQualifications; }
    public void setRequirementsAndQualifications(String requirementsAndQualifications) { this.requirementsAndQualifications = requirementsAndQualifications; }
}