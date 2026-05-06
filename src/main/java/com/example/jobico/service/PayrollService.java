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

    // ── SINGLE PAYROLL (manual API — uses numeric DB id) ─────────────────────
    @Transactional
    public PayrollResponse runPayroll(PayrollRequest request) {
        Employee emp = employeeRepository.findById(request.getEmployeeId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Employee not found: " + request.getEmployeeId()));

        if (emp.getEmployeeStatus() != EmployeeStatus.ACTIVE)
            throw new RuntimeException("Payroll can only be run for ACTIVE employees.");

        if (payrollRepository.findByEmployeeIdAndMonthAndYear(
                emp.getId(), request.getMonth(), request.getYear()).isPresent())
            throw new RuntimeException("Payroll already processed for employee "
                    + emp.getEmployeeId() + " for " + request.getMonth() + "/" + request.getYear());

        return toResponse(payrollRepository.save(buildPayroll(emp, request)));
    }

    // ── BULK PAYROLL (from Excel) ─────────────────────────────────────────────
    // Each PayrollRequest coming from ExcelParsingService has employeeIdStr set
    // (the human-readable "EMP-XXXX" value from column A).
    // We must NOT use findById(Long) here — that is the DB primary key, not the
    // business-level employeeId stored in the employees.employee_id column.
    @Transactional
    public BulkUploadResponse runBulkPayroll(List<PayrollRequest> requests) {
        List<PayrollResponse> processed = new ArrayList<>();
        List<String> errors = new ArrayList<>();

        for (int i = 0; i < requests.size(); i++) {
            PayrollRequest req = requests.get(i);
            String empIdStr = req.getEmployeeIdStr();   // e.g. "EMP-ABC123"
            try {
                // ✅ FIX: look up by the String employeeId column, not the Long PK.
                Employee emp = employeeRepository.findByEmployeeId(empIdStr)
                        .orElseThrow(() -> new ResourceNotFoundException(
                                "Employee ID \"" + empIdStr + "\" not found. "
                                + "Make sure column A contains the EMP-XXXX code, not a number."));

                if (emp.getEmployeeStatus() != EmployeeStatus.ACTIVE)
                    throw new RuntimeException("Employee " + empIdStr + " is not ACTIVE");

                if (payrollRepository.findByEmployeeIdAndMonthAndYear(
                        emp.getId(), req.getMonth(), req.getYear()).isPresent())
                    throw new RuntimeException("Payroll already exists for employee "
                            + empIdStr + " — " + req.getMonth() + "/" + req.getYear());

                processed.add(toResponse(payrollRepository.save(buildPayroll(emp, req))));

            } catch (Exception e) {
                errors.add("Row " + (i + 2) + " (EmpID " + empIdStr + "): " + e.getMessage());
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

        if (employeeId != null) {
            spec = spec.and((root, query, cb) ->
                    cb.equal(root.get("employee").get("id"), employeeId));
        }
        if (year != null) {
            spec = spec.and((root, query, cb) ->
                    cb.equal(root.get("year"), year));
        }
        // ✅ FIX: month is an int column — parse to Integer, do NOT use cb.lower()
        if (month != null && !month.isBlank()) {
            try {
                int monthInt = Integer.parseInt(month.trim());
                spec = spec.and((root, query, cb) ->
                        cb.equal(root.get("month"), monthInt));
            } catch (NumberFormatException ignored) {
                // invalid month value — ignore filter rather than crash
            }
        }

        return payrollRepository.findAll(spec, pageable).map(this::toResponse);
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
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Payroll record not found: " + payrollId));
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
        p.setMonthOrder(req.getMonth());   // ✅ FIX: keep monthOrder in sync
        p.setPaymentStatus(PaymentStatus.PAID);
        p.setPaymentDate(LocalDate.now());
        return p;
    }

    // ── HELPER: Entity → DTO ──────────────────────────────────────────────────
    private PayrollResponse toResponse(Payroll p) {
        Employee e = p.getEmployee();
        Candidate c = e.getCandidate();
        PayrollResponse r = new PayrollResponse();
        r.setId(p.getId());
        r.setEmployeeId(e.getEmployeeId());
        r.setEmployeeName(c != null
                ? c.getFirstName() + " " + c.getSurname()
                : "Unknown");          
        r.setDepartment(e.getDepartment());
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
    @Transactional
    public BulkUploadResponse runBulkPayroll(List<PayrollRequest> requests) {
        List<PayrollResponse> processed = new ArrayList<>();
        List<String> errors = new ArrayList<>();

        // ✅ Collect all empIdStr values first
        List<String> empIdStrs = requests.stream()
                .map(PayrollRequest::getEmployeeIdStr)
                .filter(id -> id != null && !id.isBlank())
                .distinct()
                .collect(Collectors.toList());

        // ✅ Single query instead of one per row
        Map<String, Employee> employeeMap = employeeRepository
                .findByEmployeeIdIn(empIdStrs)
                .stream()
                .collect(Collectors.toMap(Employee::getEmployeeId, e -> e));

        for (int i = 0; i < requests.size(); i++) {
            PayrollRequest req = requests.get(i);
            String empIdStr = req.getEmployeeIdStr();
            try {
                if (empIdStr == null || empIdStr.isBlank())
                    throw new RuntimeException("Employee ID is missing in row " + (i + 2));

                Employee emp = employeeMap.get(empIdStr);
                if (emp == null)
                    throw new ResourceNotFoundException(
                            "Employee ID \"" + empIdStr + "\" not found. "
                            + "Make sure column A contains the EMP-XXXX code.");

                if (emp.getEmployeeStatus() != EmployeeStatus.ACTIVE)
                    throw new RuntimeException("Employee " + empIdStr + " is not ACTIVE");

                if (payrollRepository.findByEmployeeIdAndMonthAndYear(
                        emp.getId(), req.getMonth(), req.getYear()).isPresent())
                    throw new RuntimeException("Payroll already exists for "
                            + empIdStr + " — " + req.getMonth() + "/" + req.getYear());

                processed.add(toResponse(payrollRepository.save(buildPayroll(emp, req))));

            } catch (Exception e) {
                errors.add("Row " + (i + 2) + " (EmpID " + empIdStr + "): " + e.getMessage());
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
}