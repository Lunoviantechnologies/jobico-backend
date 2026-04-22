package com.example.jobico.dto;

import jakarta.validation.constraints.NotBlank;

public class EducationDTO {

    private Long id;

    @NotBlank(message = "Education level is required")
    private String level;

    private int year;
    private String college;
    private double percentage;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getLevel() { return level; }
    public void setLevel(String level) { this.level = level; }
    public int getYear() { return year; }
    public void setYear(int year) { this.year = year; }
    public String getCollege() { return college; }
    public void setCollege(String college) { this.college = college; }
    public double getPercentage() { return percentage; }
    public void setPercentage(double percentage) { this.percentage = percentage; }
}