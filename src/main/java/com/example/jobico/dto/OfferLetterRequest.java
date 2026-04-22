package com.example.jobico.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.LocalDate;

public class OfferLetterRequest {

    @NotNull(message = "Candidate ID is required")
    private Long candidateId;

    @Positive(message = "Salary must be positive")
    private double salary;

    @NotNull(message = "Joining date is required")
    private LocalDate joiningDate;

    public Long getCandidateId() { return candidateId; }
    public void setCandidateId(Long candidateId) { this.candidateId = candidateId; }
    public double getSalary() { return salary; }
    public void setSalary(double salary) { this.salary = salary; }
    public LocalDate getJoiningDate() { return joiningDate; }
    public void setJoiningDate(LocalDate joiningDate) { this.joiningDate = joiningDate; }
}