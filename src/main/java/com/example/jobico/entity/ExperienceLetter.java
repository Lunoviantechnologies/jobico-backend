package com.example.jobico.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "experience_letters", indexes = {
        @Index(name = "idx_el_employee", columnList = "employee_id"),
        @Index(name = "idx_el_generated_at", columnList = "generated_at")
})
public class ExperienceLetter {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @Column(columnDefinition = "TEXT")
    private String remarks;

    @Column(nullable = false, length = 512)
    private String pdfUrl;

    @Column(nullable = false, unique = true, length = 100)
    private String referenceNumber;

    @Column(nullable = false)
    private boolean emailSent = false;

    private LocalDateTime emailSentAt;

    /** Captured at generation time so it survives even if employee record changes */
    private LocalDate lastWorkingDay;

    @Column(nullable = false, updatable = false)
    private LocalDateTime generatedAt = LocalDateTime.now();

    @Column(length = 120)
    private String generatedBy;

    // ── Getters / Setters 
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Employee getEmployee() { return employee; }
    public void setEmployee(Employee employee) { this.employee = employee; }

    public String getRemarks() { return remarks; }
    public void setRemarks(String remarks) { this.remarks = remarks; }

    public String getPdfUrl() { return pdfUrl; }
    public void setPdfUrl(String pdfUrl) { this.pdfUrl = pdfUrl; }

    public String getReferenceNumber() { return referenceNumber; }
    public void setReferenceNumber(String referenceNumber) { this.referenceNumber = referenceNumber; }

    public boolean isEmailSent() { return emailSent; }
    public void setEmailSent(boolean emailSent) { this.emailSent = emailSent; }

    public LocalDateTime getEmailSentAt() { return emailSentAt; }
    public void setEmailSentAt(LocalDateTime emailSentAt) { this.emailSentAt = emailSentAt; }

    public LocalDate getLastWorkingDay() { return lastWorkingDay; }
    public void setLastWorkingDay(LocalDate lastWorkingDay) { this.lastWorkingDay = lastWorkingDay; }

    public LocalDateTime getGeneratedAt() { return generatedAt; }
    public void setGeneratedAt(LocalDateTime generatedAt) { this.generatedAt = generatedAt; }

    public String getGeneratedBy() { return generatedBy; }
    public void setGeneratedBy(String generatedBy) { this.generatedBy = generatedBy; }
}