package com.example.jobico.dto;

import com.example.jobico.entity.ExperienceLetter;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class ExperienceLetterResponse {

    private Long        id;
    private Long        employeeId;
    private String      employeeCode;
    private String      employeeName;
    private String      employeeEmail;
    private String      role;
    private String      department;
    private LocalDate   joiningDate;
    private LocalDate   lastWorkingDay;
    private String      referenceNumber;
    private boolean     emailSent;
    private LocalDateTime emailSentAt;
    private LocalDateTime generatedAt;
    private String      generatedBy;

    public static ExperienceLetterResponse from(ExperienceLetter el) {
        ExperienceLetterResponse r = new ExperienceLetterResponse();
        r.id             = el.getId();
        r.employeeId     = el.getEmployee().getId();
        r.employeeCode   = el.getEmployee().getEmployeeId();
        r.employeeName   = el.getEmployee().getCandidate().getFirstName()
                         + " " + el.getEmployee().getCandidate().getSurname();
        r.employeeEmail  = el.getEmployee().getCandidate().getEmail();
        r.role           = el.getEmployee().getCandidate().getRole();
        r.department     = el.getEmployee().getDepartment();
        r.joiningDate    = el.getEmployee().getJoiningDate();
        r.lastWorkingDay = el.getLastWorkingDay();
        r.referenceNumber = el.getReferenceNumber();
        r.emailSent      = el.isEmailSent();
        r.emailSentAt    = el.getEmailSentAt();
        r.generatedAt    = el.getGeneratedAt();
        r.generatedBy    = el.getGeneratedBy();
        return r;
    }

    public Long getId() { return id; }
    public Long getEmployeeId() { return employeeId; }
    public String getEmployeeCode() { return employeeCode; }
    public String getEmployeeName() { return employeeName; }
    public String getEmployeeEmail() { return employeeEmail; }
    public String getRole() { return role; }
    public String getDepartment() { return department; }
    public LocalDate getJoiningDate() { return joiningDate; }
    public LocalDate getLastWorkingDay() { return lastWorkingDay; }
    public String getReferenceNumber() { return referenceNumber; }
    public boolean isEmailSent() { return emailSent; }
    public LocalDateTime getEmailSentAt() { return emailSentAt; }
    public LocalDateTime getGeneratedAt() { return generatedAt; }
    public String getGeneratedBy() { return generatedBy; }
}