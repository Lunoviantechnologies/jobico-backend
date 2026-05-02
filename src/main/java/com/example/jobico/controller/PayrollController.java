package com.example.jobico.controller;

import com.example.jobico.dto.*;
import com.example.jobico.entity.Employee;
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

    @Autowired private PayrollService       payrollService;
    @Autowired private ExcelParsingService  excelParsingService;
    @Autowired private PayslipPdfService    payslipPdfService;
    @Autowired private EmployeeRepository   employeeRepository;  
    @Autowired private DocumentService documentService;
    

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
     * POST /api/admin/payroll/upload
     * Upload Excel → auto-generate payrolls for ALL rows.
     *
     * Excel format (no header changes needed):
     * | employeeId | month | year | basicSalary | hra | allowances | deductions |
     */
    @PostMapping(value = "/api/admin/payroll/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<BulkUploadResponse> uploadPayrollExcel(
            @RequestParam("file") MultipartFile file) {
        try {
            if (file.isEmpty())
                return ResponseEntity.badRequest().build();

            String filename = file.getOriginalFilename();
            if (filename == null || (!filename.endsWith(".xlsx") && !filename.endsWith(".xls")))
                return ResponseEntity.badRequest().build();

            List<PayrollRequest> rows = excelParsingService.parsePayrollExcel(file);
            BulkUploadResponse result = payrollService.runBulkPayroll(rows);

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * GET /api/admin/payroll/payslips/{employeeId}
     * All payslips for a given employee.
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
    //  EMPLOYEE ENDPOINTS  — /api/employee/payroll/**
    //  Security: employee sees ONLY their own data (enforced by service layer)
    // ═══════════════════════════════════════════════════════════════════

    /**
     * GET /api/employee/payroll/my-payslips
     * Returns list of the authenticated employee's payslips.
     */
    @GetMapping("/api/employee/payroll/my-payslips")
    public ResponseEntity<List<PayrollResponse>> getMyPayslips(Authentication auth) {
        Long employeeId = resolveEmployeeId(auth);
        return ResponseEntity.ok(payrollService.getMyPayslips(employeeId));
    }

    /**
     * GET /api/employee/payroll/my-payslips/{payrollId}/download
     * Download the authenticated employee's specific payslip PDF.
     * Service validates ownership — an employee CANNOT download another's payslip.
     */
    @GetMapping("/api/employee/payroll/my-payslips/{payrollId}/download")
    public ResponseEntity<byte[]> downloadMyPayslip(
            @PathVariable Long payrollId, Authentication auth) {
        try {
            Long employeeId = resolveEmployeeId(auth);

            // Ownership check happens inside service — throws if mismatch
            PayrollResponse payroll = payrollService.getPayrollByIdForEmployee(payrollId, employeeId);
            byte[] pdf = payslipPdfService.generatePayslipPdf(payroll);

            String filename = "MyPayslip_" + payroll.getMonth() + "_" + payroll.getYear() + ".pdf";

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(pdf);

        } catch (RuntimeException e) {
            // Return 403 if ownership check fails
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    private Long resolveEmployeeId(Authentication auth) {
        String mobile = auth.getName();
        return employeeRepository.findByUserMobile(mobile)
                .orElseThrow(() -> new RuntimeException("No employee record linked to this account"))
                .getId();
    }
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
    @GetMapping("/api/employee/my-experience-letter")
    public ResponseEntity<byte[]> downloadMyExperienceLetter(Authentication auth) {
        try {
            String mobile = auth.getName();
            Employee emp = employeeRepository.findByUserMobile(mobile)
                    .orElseThrow(() -> new RuntimeException("No employee record linked to this account"));

            ExperienceLetterRequest req = new ExperienceLetterRequest();
            req.setEmployeeId(emp.getId());
            // remarks optional — pass null or empty, DocumentService handles it gracefully
            req.setRemarks(null);

            byte[] pdf = documentService.generateExperienceLetter(req);

            String filename = "ExperienceLetter_" + emp.getCandidate().getFirstName() + ".pdf";
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
}