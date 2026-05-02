package com.example.jobico.dto;

import com.example.jobico.entity.JobPostStatus;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public class JobPostResponse {

    private Long id;
    private String title;
    private String category;
    private String jobType;
    private String workType;
    private String location;
    private String experienceRequired;
    private int numberOfOpenings;
    private Double salaryMin;
    private Double salaryMax;
    private LocalDate applicationDeadline;
    private List<String> techStack;

    private String description;
    private String responsibilities;
    private String requirementsAndQualifications;
    private JobPostStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String postedAgo;

    // ─── Getters & Setters 
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

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

    public JobPostStatus getStatus() { return status; }
    public void setStatus(JobPostStatus status) { this.status = status; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public String getPostedAgo() { return postedAgo; }
    public void setPostedAgo(String postedAgo) { this.postedAgo = postedAgo; }
}