package com.example.jobico.service;

import com.example.jobico.dto.*;
import com.example.jobico.entity.*;
import com.example.jobico.exception.ResourceNotFoundException;
import com.example.jobico.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Pageable;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class PayrollService {

    @Autowired private PayrollRepository payrollRepository;
    @Autowired private EmployeeRepository employeeRepository;

    // ── SINGLE PAYROLL ────────────────────────────────────────────────────────
    @Transactional
    public PayrollResponse runPayroll(PayrollRequest request) {
        Employee emp = employeeRepository.findById(request.getEmployeeId())
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found: " + request.getEmployeeId()));

        if (emp.getEmployeeStatus() != EmployeeStatus.ACTIVE)
            throw new RuntimeException("Payroll can only be run for ACTIVE employees.");

        if (payrollRepository.findByEmployeeIdAndMonthAndYear(
                emp.getId(), request.getMonth(), request.getYear()).isPresent())
            throw new RuntimeException("Payroll already processed for employee "
                    + emp.getId() + " for " + request.getMonth() + "/" + request.getYear());

        return toResponse(payrollRepository.save(buildPayroll(emp, request)));
    }

    // ── BULK PAYROLL (from Excel) ─────────────────────────────────────────────
    @Transactional
    public BulkUploadResponse runBulkPayroll(List<PayrollRequest> requests) {
        List<PayrollResponse> processed = new ArrayList<>();
        List<String> errors = new ArrayList<>();

        for (int i = 0; i < requests.size(); i++) {
            PayrollRequest req = requests.get(i);
            try {
                Employee emp = employeeRepository.findById(req.getEmployeeId())
                        .orElseThrow(() -> new ResourceNotFoundException(
                                "Employee ID " + req.getEmployeeId() + " not found"));

                if (emp.getEmployeeStatus() != EmployeeStatus.ACTIVE)
                    throw new RuntimeException("Employee " + req.getEmployeeId() + " is not ACTIVE");

                if (payrollRepository.findByEmployeeIdAndMonthAndYear(
                        emp.getId(), req.getMonth(), req.getYear()).isPresent())
                    throw new RuntimeException("Payroll already exists for employee "
                            + req.getEmployeeId() + " — " + req.getMonth() + "/" + req.getYear());

                processed.add(toResponse(payrollRepository.save(buildPayroll(emp, req))));

            } catch (Exception e) {
                errors.add("Row " + (i + 2) + " (EmpID " + req.getEmployeeId() + "): " + e.getMessage());
            }
        }

        return new BulkUploadResponse(
                requests.size(),
                processed.size(),
                errors.size(),
                errors,
                processed
        );
    }
    
    public Page<PayrollResponse> getAllPayslips(
            Long employeeId,
            Integer year,
            String month,
            int page,
            int size) {
        int safePage = Math.max(page, 0);
        int safeSize = (size <= 0 || size > 100) ? 10 : size;

        Pageable pageable = PageRequest.of(
                safePage,
                safeSize,
                Sort.by(
                        Sort.Order.desc("year"),
                        Sort.Order.desc("month")
                )
        );

        Specification<Payroll> spec = Specification.where(null);

        // ✅ Filter: employeeId
        if (employeeId != null) {
            spec = spec.and((root, query, cb) ->
                    cb.equal(root.get("employee").get("id"), employeeId));
        }
        if (year != null) {
            spec = spec.and((root, query, cb) ->
                    cb.equal(root.get("year"), year));
        }
        if (month != null && !month.isBlank()) {
            spec = spec.and((root, query, cb) ->
                    cb.equal(
                            cb.lower(root.get("month")),
                            month.trim().toLowerCase()
                    ));
        }

      
        Page<Payroll> payrollPage = payrollRepository.findAll(spec, pageable);

        return payrollPage.map(this::toResponse);
    }

    // ── ADMIN: ALL PAYSLIPS FOR EMPLOYEE ─────────────────────────────────────
    public List<PayrollResponse> getPayslips(Long employeeId) {
        if (!employeeRepository.existsById(employeeId))
            throw new ResourceNotFoundException("Employee not found");
        return payrollRepository.findByEmployeeId(employeeId)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    // ── EMPLOYEE: ONLY THEIR OWN PAYSLIPS ────────────────────────────────────
    public List<PayrollResponse> getMyPayslips(Long employeeId) {
        return payrollRepository.findByEmployeeId(employeeId)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    // ── GET SINGLE PAYROLL (for PDF generation) ───────────────────────────────
    public PayrollResponse getPayrollById(Long payrollId) {
        Payroll p = payrollRepository.findById(payrollId)
                .orElseThrow(() -> new ResourceNotFoundException("Payroll record not found: " + payrollId));
        return toResponse(p);
    }

    // ── EMPLOYEE: VALIDATE OWNERSHIP BEFORE DOWNLOAD ─────────────────────────
    public PayrollResponse getPayrollByIdForEmployee(Long payrollId, Long employeeId) {
        Payroll p = payrollRepository.findById(payrollId)
                .orElseThrow(() -> new ResourceNotFoundException("Payroll record not found"));
        if (!p.getEmployee().getId().equals(employeeId))
            throw new RuntimeException("Access denied: This payslip does not belong to you.");
        return toResponse(p);
    }

    // ── HELPER: Build Payroll entity ─────────────────────────────────────────
    private Payroll buildPayroll(Employee emp, PayrollRequest req) {
        double net = req.getBasicSalary() + req.getHra()
                   + req.getAllowances() - req.getDeductions();
        Payroll p = new Payroll();
        p.setEmployee(emp);
        p.setMonth(req.getMonth());
        p.setYear(req.getYear());
        p.setBasicSalary(req.getBasicSalary());
        p.setHra(req.getHra());
        p.setAllowances(req.getAllowances());
        p.setDeductions(req.getDeductions());
        p.setNetSalary(net);
        p.setPaymentStatus(PaymentStatus.PAID);
        p.setPaymentDate(LocalDate.now());
        return p;
    }

    // ── HELPER: Entity → DTO ──────────────────────────────────────────────────
    private PayrollResponse toResponse(Payroll p) {
        PayrollResponse r = new PayrollResponse();
        r.setId(p.getId());
        r.setEmployeeId(p.getEmployee().getEmployeeId());
        r.setEmployeeName(p.getEmployee().getCandidate().getFirstName()
                + " " + p.getEmployee().getCandidate().getSurname());
        r.setDepartment(p.getEmployee().getDepartment());
        r.setMonth(p.getMonth());
        r.setYear(p.getYear());
        r.setBasicSalary(p.getBasicSalary());
        r.setHra(p.getHra());
        r.setAllowances(p.getAllowances());
        r.setDeductions(p.getDeductions());
        r.setNetSalary(p.getNetSalary());
        r.setPaymentStatus(p.getPaymentStatus());
        r.setPaymentDate(p.getPaymentDate());
        return r;
    }
}