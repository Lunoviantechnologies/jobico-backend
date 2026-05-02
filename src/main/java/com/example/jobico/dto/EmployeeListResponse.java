package com.example.jobico.dto;

import com.example.jobico.entity.EmployeeStatus;
import java.time.LocalDate;

public class EmployeeListResponse {

    private Long id;
    private String employeeId;
    private String name;
    private String role;
    private String department;
    private LocalDate joiningDate;
    private EmployeeStatus status;
    private String bankName;
    private String bankAccountMasked; // e.g. "HDFC ****4521"
    private String mobile;
    private String email;
    private double salary;
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getEmployeeId() { return employeeId; }
    public void setEmployeeId(String employeeId) { this.employeeId = employeeId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }

    public LocalDate getJoiningDate() { return joiningDate; }
    public void setJoiningDate(LocalDate joiningDate) { this.joiningDate = joiningDate; }

    public EmployeeStatus getStatus() { return status; }
    public void setStatus(EmployeeStatus status) { this.status = status; }

    public String getBankName() { return bankName; }
    public void setBankName(String bankName) { this.bankName = bankName; }

    public String getBankAccountMasked() { return bankAccountMasked; }
    public void setBankAccountMasked(String bankAccountMasked) { this.bankAccountMasked = bankAccountMasked; }

    public String getMobile() { return mobile; }
    public void setMobile(String mobile) { this.mobile = mobile; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
	public double getSalary() {return salary;}
	public void setSalary(double salary) {this.salary = salary;}
    
}
