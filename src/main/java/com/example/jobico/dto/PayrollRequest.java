package com.example.jobico.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public class PayrollRequest {

    // ── Used by the manual single-run API (admin sends the DB primary key as Long) ──
    @NotNull(message = "Employee ID is required")
    private Long employeeId;

    // ✅ NEW: Used by the Excel bulk-upload path.
    // Excel column A contains the human-readable employeeId string (e.g. "EMP-ABC123").
    // ExcelParsingService sets this field; PayrollService reads it to look up the employee
    // via EmployeeRepository.findByEmployeeId(String).
    private String employeeIdStr;

    @Min(value = 1, message = "Month must be between 1 and 12")
    @Max(value = 12, message = "Month must be between 1 and 12")
    private int month;

    @Min(value = 2000, message = "Invalid year")
    private int year;

    private double basicSalary;
    private double hra;
    private double allowances;
    private double deductions;

    public Long getEmployeeId() { return employeeId; }
    public void setEmployeeId(Long employeeId) { this.employeeId = employeeId; }

    public String getEmployeeIdStr() { return employeeIdStr; }
    public void setEmployeeIdStr(String employeeIdStr) { this.employeeIdStr = employeeIdStr; }

    public int getMonth() { return month; }
    public void setMonth(int month) { this.month = month; }
    public int getYear() { return year; }
    public void setYear(int year) { this.year = year; }
    public double getBasicSalary() { return basicSalary; }
    public void setBasicSalary(double basicSalary) { this.basicSalary = basicSalary; }
    public double getHra() { return hra; }
    public void setHra(double hra) { this.hra = hra; }
    public double getAllowances() { return allowances; }
    public void setAllowances(double allowances) { this.allowances = allowances; }
    public double getDeductions() { return deductions; }
    public void setDeductions(double deductions) { this.deductions = deductions; }
}