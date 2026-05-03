package com.example.jobico.dto;

import com.example.jobico.entity.OfferLetter;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** Metadata returned to the admin — never contains PDF bytes. */
public class OfferLetterResponse {

    private Long        id;
    private Long        candidateId;
    private String      candidateName;
    private String      candidateEmail;
    private String      role;
    private double      salary;
    private LocalDate   joiningDate;
    private String      referenceNumber;
    private boolean     emailSent;
    private LocalDateTime emailSentAt;
    private LocalDateTime generatedAt;
    private String      generatedBy;

    public static OfferLetterResponse from(OfferLetter ol) {
        OfferLetterResponse r = new OfferLetterResponse();
        r.id              = ol.getId();
        r.candidateId     = ol.getCandidate().getId();
        r.candidateName   = ol.getCandidate().getFirstName() + " " + ol.getCandidate().getSurname();
        r.candidateEmail  = ol.getCandidate().getEmail();
        r.role            = ol.getCandidate().getRole();
        r.salary          = ol.getSalary();
        r.joiningDate     = ol.getJoiningDate();
        r.referenceNumber = ol.getReferenceNumber();
        r.emailSent       = ol.isEmailSent();
        r.emailSentAt     = ol.getEmailSentAt();
        r.generatedAt     = ol.getGeneratedAt();
        r.generatedBy     = ol.getGeneratedBy();
        return r;
    }

    public Long getId() { return id; }
    public Long getCandidateId() { return candidateId; }
    public String getCandidateName() { return candidateName; }
    public String getCandidateEmail() { return candidateEmail; }
    public String getRole() { return role; }
    public double getSalary() { return salary; }
    public LocalDate getJoiningDate() { return joiningDate; }
    public String getReferenceNumber() { return referenceNumber; }
    public boolean isEmailSent() { return emailSent; }
    public LocalDateTime getEmailSentAt() { return emailSentAt; }
    public LocalDateTime getGeneratedAt() { return generatedAt; }
    public String getGeneratedBy() { return generatedBy; }
}