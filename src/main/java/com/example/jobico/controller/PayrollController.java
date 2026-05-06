package com.example.jobico.controller;

import com.example.jobico.dto.*;
import com.example.jobico.entity.Employee;
import com.example.jobico.exception.ResourceNotFoundException;
import com.example.jobico.repository.EmployeeRepository;
import com.example.jobico.service.*;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
public class PayrollController {

    @Autowired private PayrollService      payrollService;
    @Autowired private ExcelParsingService excelParsingService;
    @Autowired private PayslipPdfService   payslipPdfService;
    @Autowired private EmployeeRepository  employeeRepository;
    @Autowired private DocumentService     documentService;

    // ═══════════════════════════════════════════════════════════════════
    //  ADMIN ENDPOINTS — /api/admin/payroll/**
    // ═══════════════════════════════════════════════════════════════════

    /**
     * POST /api/admin/payroll/run
     * Single employee payroll run.
     */
    @PostMapping("/api/admin/payroll/run")
    public ResponseEntity<PayrollResponse> runPayroll(
            @Valid @RequestBody PayrollRequest request) {
        return ResponseEntity.ok(payrollService.runPayroll(request));
    }

    /**
     * POST /api/admin/payroll/run/bulk
     * Run payroll for multiple employees via JSON list.
     */
    @PostMapping("/api/admin/payroll/run/bulk")
    public ResponseEntity<BulkUploadResponse> runBulkPayroll(
            @Valid @RequestBody List<PayrollRequest> requests) {
        if (requests == null || requests.isEmpty())
            return ResponseEntity.badRequest().build();
        return ResponseEntity.ok(payrollService.runBulkPayroll(requests));
    }

    /**
     * POST /api/admin/payroll/upload
     * Upload Excel -> auto-generate payrolls.
     *
     * With month & year params → Excel needs: employeeId, basicSalary, hra, allowances, deductions
     * Without params           → Excel needs: employeeId, month, year, basicSalary, hra, allowances, deductions
     */
    @PostMapping(value = "/api/admin/payroll/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<BulkUploadResponse> uploadPayrollExcel(
            @RequestParam("file") MultipartFile file,
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) Integer year) {
        try {
            if (file.isEmpty())
                return ResponseEntity.badRequest().build();

            String filename = file.getOriginalFilename();
            if (filename == null || (!filename.endsWith(".xlsx") && !filename.endsWith(".xls")))
                return ResponseEntity.badRequest().build();

            List<PayrollRequest> rows = excelParsingService.parsePayrollExcel(
                    file,
                    month != null ? month : 0,
                    year  != null ? year  : 0
            );

            return ResponseEntity.ok(payrollService.runBulkPayroll(rows));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * GET /api/admin/payroll/payslips
     * Paginated list of all payslips - optional filters: employeeId, year, month.
     */
    @GetMapping("/api/admin/payroll/payslips")
    public ResponseEntity<Page<PayrollResponse>> getAllPayslips(
            @RequestParam(required = false) Long employeeId,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) String month,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(
                payrollService.getAllPayslips(employeeId, year, month, page, size)
        );
    }

    /**
     * GET /api/admin/payroll/payslips/{employeeId}
     * All payslips for a specific employee.
     */
    @GetMapping("/api/admin/payroll/payslips/{employeeId}")
    public ResponseEntity<List<PayrollResponse>> getPayslips(
            @PathVariable Long employeeId) {
        return ResponseEntity.ok(payrollService.getPayslips(employeeId));
    }

    /**
     * GET /api/admin/payroll/payslip/{payrollId}/download
     * Download any employee's payslip as PDF.
     */
    @GetMapping("/api/admin/payroll/payslip/{payrollId}/download")
    public ResponseEntity<byte[]> downloadPayslipAdmin(
            @PathVariable Long payrollId) {
        try {
            PayrollResponse payroll = payrollService.getPayrollById(payrollId);
            byte[] pdf = payslipPdfService.generatePayslipPdf(payroll);

            String filename = "Payslip_" + payroll.getEmployeeId()
                    + "_" + payroll.getMonth() + "_" + payroll.getYear() + ".pdf";

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(pdf);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    //  EMPLOYEE ENDPOINTS — /api/employee/**
    // ═══════════════════════════════════════════════════════════════════

    /**
     * GET /api/employee/payroll/my-payslips
     */
    @GetMapping("/api/employee/payroll/my-payslips")
    public ResponseEntity<List<PayrollResponse>> getMyPayslips(Authentication auth) {
        Long employeeId = resolveEmployeeId(auth);
        return ResponseEntity.ok(payrollService.getMyPayslips(employeeId));
    }

    /**
     * GET /api/employee/payroll/my-payslips/{payrollId}/download
     */
    @GetMapping("/api/employee/payroll/my-payslips/{payrollId}/download")
    public ResponseEntity<byte[]> downloadMyPayslip(
            @PathVariable Long payrollId, Authentication auth) {
        try {
            Long employeeId = resolveEmployeeId(auth);
            PayrollResponse payroll = payrollService.getPayrollByIdForEmployee(payrollId, employeeId);
            byte[] pdf = payslipPdfService.generatePayslipPdf(payroll);

            String filename = "MyPayslip_" + payroll.getMonth() + "_" + payroll.getYear() + ".pdf";

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(pdf);

        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * GET /api/employee/my-experience-letter
     */
    @GetMapping("/api/employee/my-experience-letter")
    public ResponseEntity<byte[]> downloadMyExperienceLetter(Authentication auth) {
        try {
            Employee emp = resolveEmployee(auth);
            byte[] pdf = documentService.downloadLatestExperienceLetterForEmployee(emp.getId());

            String firstName = (emp.getCandidate() != null && emp.getCandidate().getFirstName() != null)
                    ? emp.getCandidate().getFirstName()
                    : String.valueOf(emp.getId());
            String filename = "ExperienceLetter_" + firstName + ".pdf";

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(pdf);

        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    //  PRIVATE HELPERS
    // ═══════════════════════════════════════════════════════════════════

    private Employee resolveEmployee(Authentication auth) {
        String mobile = auth.getName();
        return employeeRepository.findByUserMobile(mobile)
                .orElseThrow(() -> new IllegalArgumentException(
                        "No employee record linked to account: " + mobile));
    }

    private Long resolveEmployeeId(Authentication auth) {
        return resolveEmployee(auth).getId();
    }
}