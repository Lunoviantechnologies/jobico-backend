package com.example.jobico.service;

import com.example.jobico.dto.DashboardStatsResponse;
import com.example.jobico.entity.CandidateStatus;
import com.example.jobico.entity.EmployeeStatus;
import com.example.jobico.repository.CandidateRepository;
import com.example.jobico.repository.EmployeeRepository;
import com.example.jobico.repository.PayrollRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
public class DashboardService {

    @Autowired private CandidateRepository candidateRepository;
    @Autowired private EmployeeRepository employeeRepository;
    @Autowired private PayrollRepository payrollRepository;

    public DashboardStatsResponse getStats() {
        DashboardStatsResponse stats = new DashboardStatsResponse();

        // Total candidates & pipeline
        long total       = candidateRepository.count();
        long applied     = candidateRepository.countByStatus(CandidateStatus.APPLIED);
        long shortlisted = candidateRepository.countByStatus(CandidateStatus.SHORTLISTED);
        long selected    = candidateRepository.countByStatus(CandidateStatus.SELECTED);
        long rejected    = candidateRepository.countByStatus(CandidateStatus.REJECTED);

        stats.setTotalCandidates(total);
        stats.setTotalSelected(selected);
        stats.setApplied(applied);
        stats.setShortlisted(shortlisted);
        stats.setSelected(selected);
        stats.setRejected(rejected);

        // Active employees (onboarded)
        long activeEmployees = employeeRepository.countByEmployeeStatus(EmployeeStatus.ACTIVE);
        stats.setTotalOnboarded(activeEmployees);
        stats.setTotalActiveEmployees(activeEmployees);

        // Payroll total for current month
        LocalDate now = LocalDate.now();
        double payrollThisMonth = payrollRepository.sumNetSalaryByMonthAndYear(now.getMonthValue(), now.getYear());
        stats.setTotalPayrollThisMonth(payrollThisMonth);

        return stats;
    }
}
