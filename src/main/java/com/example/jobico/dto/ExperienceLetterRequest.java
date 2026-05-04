package com.example.jobico.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.NotNull;

public class ExperienceLetterRequest {

    @NotNull(message = "Employee ID is required")
    private Long employeeId;

    private String remarks;
    
    @NotNull
    private LocalDate   lastWorkingDay;

    public Long getEmployeeId() { return employeeId; }
    public void setEmployeeId(Long employeeId) { this.employeeId = employeeId; }
    public String getRemarks() { return remarks; }
    public void setRemarks(String remarks) { this.remarks = remarks; }
	public LocalDate getLastWorkingDay() {
		return lastWorkingDay;
	}
	public void setLastWorkingDay(LocalDate lastWorkingDay) {
		this.lastWorkingDay = lastWorkingDay;
	}
    
}