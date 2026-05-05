package com.example.jobico.service;

import com.example.jobico.dto.*;
import com.example.jobico.entity.*;
import com.example.jobico.exception.ResourceNotFoundException;
import com.example.jobico.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class EmployeeService {

    @Autowired private EmployeeRepository employeeRepository;
    @Autowired private CandidateRepository candidateRepository;

    @Transactional
    public OnboardingResponse onboard(OnboardingRequest request) {
        Candidate candidate = candidateRepository.findById(request.getCandidateId())
                .orElseThrow(() -> new ResourceNotFoundException("Candidate not found"));

        // ✅ FIX 1: Also allow ONBOARDED status.
        // Previously, if the candidate was already marked ONBOARDED (e.g. from a prior
        // partial save or a status update), the check below would throw
        // "Employee already onboarded" even when no Employee row actually exists yet.
        // We now allow SELECTED, OFFER_LETTER_GENERATED, and ONBOARDED through this gate.
        if (candidate.getStatus() != CandidateStatus.SELECTED
                && candidate.getStatus() != CandidateStatus.OFFER_LETTER_GENERATED
                && candidate.getStatus() != CandidateStatus.ONBOARDED) {
            throw new RuntimeException(
                    "Onboarding allowed only for candidates with status SELECTED, "
                    + "OFFER_LETTER_GENERATED, or ONBOARDED.");
        }

        if (employeeRepository.existsByCandidateId(candidate.getId())) {
            Employee existing = employeeRepository.findByCandidateId(candidate.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Employee record inconsistency"));
            return toResponse(existing);
        }

        Employee emp = new Employee();
        emp.setCandidate(candidate);
        emp.setEmployeeId("EMP-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        emp.setDepartment(request.getDepartment());
        emp.setJoiningDate(request.getJoiningDate());
        emp.setDocumentsVerified(request.isDocumentsVerified());
        emp.setBankAccountNumber(request.getBankAccountNumber());
        emp.setBankName(request.getBankName());
        emp.setIfscCode(request.getIfscCode());
        emp.setEmployeeStatus(EmployeeStatus.ACTIVE);

        Employee savedEmp = employeeRepository.save(emp);

        // Mark candidate as onboarded only if not already
        if (candidate.getStatus() != CandidateStatus.ONBOARDED) {
            candidate.setStatus(CandidateStatus.ONBOARDED);
            candidateRepository.save(candidate);
        }

        return toResponse(savedEmp);
    }

    public OnboardingResponse getEmployee(Long employeeId) {
        return toResponse(employeeRepository.findById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found")));
    }

    private OnboardingResponse toResponse(Employee emp) {
        OnboardingResponse r = new OnboardingResponse();
        r.setId(emp.getId());
        r.setEmployeeId(emp.getEmployeeId());
        r.setCandidateName(emp.getCandidate().getFirstName() + " " + emp.getCandidate().getSurname());
        r.setMobile(emp.getCandidate().getUser().getMobile());
        r.setRole(emp.getCandidate().getRole());
        r.setDepartment(emp.getDepartment());
        r.setJoiningDate(emp.getJoiningDate());
        r.setDocumentsVerified(emp.isDocumentsVerified());
        r.setBankAccountNumber(emp.getBankAccountNumber());
        r.setBankName(emp.getBankName());
        r.setIfscCode(emp.getIfscCode());
        r.setEmployeeStatus(emp.getEmployeeStatus());
        return r;
    }
}