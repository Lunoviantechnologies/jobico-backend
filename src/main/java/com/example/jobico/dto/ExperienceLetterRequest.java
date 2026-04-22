package com.example.jobico.dto;

import jakarta.validation.constraints.NotNull;

public class ExperienceLetterRequest {

    @NotNull(message = "Employee ID is required")
    private Long employeeId;

    private String remarks;

    public Long getEmployeeId() { return employeeId; }
    public void setEmployeeId(Long employeeId) { this.employeeId = employeeId; }
    public String getRemarks() { return remarks; }
    public void setRemarks(String remarks) { this.remarks = remarks; }
}