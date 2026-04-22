package com.example.jobico.controller;

import com.example.jobico.dto.*;
import com.example.jobico.service.PayrollService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/payroll")
public class PayrollController {

    @Autowired private PayrollService payrollService;

    /**
     * POST /api/admin/payroll/run
     * Run monthly payroll for an employee.
     * Body: { "employeeId": 1, "month": 4, "year": 2026,
     *         "basicSalary": 40000, "hra": 16000,
     *         "allowances": 5000, "deductions": 2000 }
     * Net = basicSalary + hra + allowances - deductions
     */
    @PostMapping("/run")
    public ResponseEntity<PayrollResponse> runPayroll(
            @Valid @RequestBody PayrollRequest request) {
        return ResponseEntity.ok(payrollService.runPayroll(request));
    }

    /**
     * GET /api/admin/payroll/payslips/{employeeId}
     * Get all payslips for an employee.
     */
    @GetMapping("/payslips/{employeeId}")
    public ResponseEntity<List<PayrollResponse>> getPayslips(@PathVariable Long employeeId) {
        return ResponseEntity.ok(payrollService.getPayslips(employeeId));
    }
}