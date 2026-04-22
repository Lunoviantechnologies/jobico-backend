package com.example.jobico.service;

import com.example.jobico.dto.*;
import com.example.jobico.entity.*;
import com.example.jobico.exception.ResourceNotFoundException;
import com.example.jobico.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Service
public class DocumentService {

    @Autowired private CandidateRepository candidateRepository;
    @Autowired private EmployeeRepository employeeRepository;
    @Autowired private EmailService emailService;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd MMMM yyyy");

    /**
     * Generate Offer Letter as text content (bytes).
     * Only allowed when candidate status = SELECTED.
     */
    public byte[] generateOfferLetter(OfferLetterRequest request) {
        Candidate c = candidateRepository.findById(request.getCandidateId())
                .orElseThrow(() -> new ResourceNotFoundException("Candidate not found"));

        if (c.getStatus() != CandidateStatus.SELECTED)
            throw new RuntimeException("Offer letter can only be generated for SELECTED candidates.");

        String today   = DATE_FMT.format(LocalDate.now());
        String joining = DATE_FMT.format(request.getJoiningDate());

        String content = """
                ============================================================
                                        OFFER LETTER
                ============================================================

                Date: %s

                Dear %s %s,

                We are delighted to offer you the position of %s at Jobico.

                Details of your appointment are as follows:

                  Designation  : %s
                  Joining Date : %s
                  CTC (Annual) : Rs. %.2f

                Please sign and return a copy of this letter as your acceptance
                of the above terms and conditions.

                We look forward to welcoming you to the team.

                Warm regards,
                HR Department
                Jobico
                ============================================================
                """.formatted(
                today,
                c.getFirstName(), c.getSurname(),
                c.getRole(),
                c.getRole(),
                joining,
                request.getSalary());

        return content.getBytes();
    }

    /**
     * Generate Experience Letter as text content (bytes).
     * Can be requested by admin or on employee exit.
     */
    public byte[] generateExperienceLetter(ExperienceLetterRequest request) {
        Employee emp = employeeRepository.findById(request.getEmployeeId())
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found"));

        Candidate c = emp.getCandidate();
        String today   = DATE_FMT.format(LocalDate.now());
        String fromDate = DATE_FMT.format(emp.getJoiningDate());
        String remarks = (request.getRemarks() != null && !request.getRemarks().isBlank())
                ? request.getRemarks()
                : "We wish " + c.getFirstName() + " the very best in all future endeavors.";

        String content = """
                ============================================================
                                    EXPERIENCE LETTER
                ============================================================

                Date: %s

                To Whom It May Concern,

                This is to certify that %s %s (Employee ID: %s) was employed
                with Jobico as %s in the %s department from %s to %s.

                During their tenure, they demonstrated dedication and
                professionalism in all assigned responsibilities.

                %s

                Sincerely,
                HR Department
                Jobico
                ============================================================
                """.formatted(
                today,
                c.getFirstName(), c.getSurname(),
                emp.getEmployeeId(),
                c.getRole(),
                emp.getDepartment(),
                fromDate,
                today,
                remarks);

        return content.getBytes();
    }
}