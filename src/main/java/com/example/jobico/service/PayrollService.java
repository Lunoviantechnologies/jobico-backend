package com.example.jobico.service;

import com.example.jobico.dto.*;
import com.example.jobico.entity.*;
import com.example.jobico.exception.ResourceNotFoundException;
import com.example.jobico.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class PayrollService {

    @Autowired private PayrollRepository payrollRepository;
    @Autowired private EmployeeRepository employeeRepository;

    // ── SINGLE PAYROLL ────────────────────────────────────────────────────────
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
    @Transactional
    public BulkUploadResponse runBulkPayroll(List<PayrollRequest> requests) {
        List<PayrollResponse> processed = new ArrayList<>();
        List<String> errors = new ArrayList<>();

        if (requests == null || requests.isEmpty())
            return new BulkUploadResponse(0, 0, 0, errors, processed);

        // ✅ Batch fetch all employees in one query
        List<String> empIdStrs = requests.stream()
                .map(PayrollRequest::getEmployeeIdStr)
                .filter(id -> id != null && !id.isBlank())
                .distinct()
                .collect(Collectors.toList());

        Map<String, Employee> employeeMap = employeeRepository
                .findByEmployeeIdIn(empIdStrs)
                .stream()
                .collect(Collectors.toMap(Employee::getEmployeeId, e -> e));

        // ✅ Batch fetch existing payrolls for duplicate check — one query
        int month = requests.get(0).getMonth();
        int year  = requests.get(0).getYear();
        List<Long> employeeDbIds = new ArrayList<>(employeeMap.values())
                .stream().map(Employee::getId).collect(Collectors.toList());

        Set<Long> alreadyProcessed = payrollRepository
                .findByMonthAndYearAndEmployeeIdIn(month, year, employeeDbIds)
                .stream()
                .map(p -> p.getEmployee().getId())
                .collect(Collectors.toSet());

        for (int i = 0; i < requests.size(); i++) {
            PayrollRequest req = requests.get(i);
            String empIdStr = req.getEmployeeIdStr();
            try {
                if (empIdStr == null || empIdStr.isBlank())
                    throw new RuntimeException("Employee ID is missing in row " + (i + 2));

                Employee emp = employeeMap.get(empIdStr);
                if (emp == null)
                    throw new ResourceNotFoundException(
                            "Employee ID \"" + empIdStr + "\" not found.");

                if (emp.getEmployeeStatus() != EmployeeStatus.ACTIVE)
                    throw new RuntimeException("Employee " + empIdStr + " is not ACTIVE");

                if (alreadyProcessed.contains(emp.getId()))
                    throw new RuntimeException("Payroll already exists for "
                            + empIdStr + " — " + req.getMonth() + "/" + req.getYear());

                processed.add(toResponse(payrollRepository.save(buildPayroll(emp, req))));

            } catch (Exception e) {
                errors.add("Row " + (i + 2) + " (EmpID " + empIdStr + "): " + e.getMessage());
            }
        }

        return new BulkUploadResponse(
                requests.size(), processed.size(), errors.size(), errors, processed);
    }

    // ── ALL PAYSLIPS PAGINATED (admin) ────────────────────────────────────────
    public Page<PayrollResponse> getAllPayslips(
            Long employeeId, Integer year, String month, int page, int size) {

        int safePage = Math.max(page, 0);
        int safeSize = (size <= 0 || size > 100) ? 10 : size;

        Pageable pageable = PageRequest.of(safePage, safeSize,
                Sort.by(Sort.Order.desc("year"), Sort.Order.desc("month")));

        Specification<Payroll> spec = Specification.where(null);

        if (employeeId != null)
            spec = spec.and((root, query, cb) ->
                    cb.equal(root.get("employee").get("id"), employeeId));

        if (year != null)
            spec = spec.and((root, query, cb) ->
                    cb.equal(root.get("year"), year));

        if (month != null && !month.isBlank()) {
            try {
                int monthInt = Integer.parseInt(month.trim());
                spec = spec.and((root, query, cb) ->
                        cb.equal(root.get("month"), monthInt));
            } catch (NumberFormatException ignored) { }
        }

        return payrollRepository.findAll(spec, pageable).map(this::toResponse);
    }

    // ── ALL PAYSLIPS FOR EMPLOYEE (admin) ─────────────────────────────────────
    public List<PayrollResponse> getPayslips(Long employeeId) {
        if (!employeeRepository.existsById(employeeId))
            throw new ResourceNotFoundException("Employee not found");
        return payrollRepository.findByEmployeeId(employeeId)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    // ── OWN PAYSLIPS (employee) ───────────────────────────────────────────────
    public List<PayrollResponse> getMyPayslips(Long employeeId) {
        return payrollRepository.findByEmployeeId(employeeId)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    // ── GET SINGLE PAYROLL ────────────────────────────────────────────────────
    public PayrollResponse getPayrollById(Long payrollId) {
        return toResponse(payrollRepository.findById(payrollId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Payroll record not found: " + payrollId)));
    }

    // ── VALIDATE OWNERSHIP BEFORE DOWNLOAD ───────────────────────────────────
    public PayrollResponse getPayrollByIdForEmployee(Long payrollId, Long employeeId) {
        Payroll p = payrollRepository.findById(payrollId)
                .orElseThrow(() -> new ResourceNotFoundException("Payroll record not found"));
        if (!p.getEmployee().getId().equals(employeeId))
            throw new RuntimeException("Access denied: This payslip does not belong to you.");
        return toResponse(p);
    }

    // ── HELPER: Build Payroll entity ──────────────────────────────────────────
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
        p.setMonthOrder(req.getMonth());
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
}