 package com.example.jobico.controller;

import com.example.jobico.dto.*;
import com.example.jobico.entity.EmployeeStatus;
import com.example.jobico.service.EmployeeManagementService;
import com.example.jobico.service.EmployeeService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

/**
 * FULL REPLACEMENT for EmployeeController.
 * Adds:
 *   GET  /api/admin/employees         – list all (search, dept, status filters)
 *   PATCH /api/admin/employees/{id}/status – update employee status
 *
 * Keeps:
 *   POST /api/admin/employees/onboard
 *   GET  /api/admin/employees/{id}
 */
@RestController
@RequestMapping("/api/admin/employees")
public class EmployeeController {

    @Autowired private EmployeeService employeeService;
    @Autowired private EmployeeManagementService employeeManagementService;

    // ── Onboard ───────────────────────────────────────────────────────────────

    /**
     * POST /api/admin/employees/onboard
     * Body: { "candidateId": 1, "department": "Engineering",
     *         "joiningDate": "2026-05-01", "documentsVerified": true,
     *         "bankAccountNumber": "...", "bankName": "SBI", "ifscCode": "SBIN0001234" }
     */
    @PostMapping("/onboard")
    public ResponseEntity<OnboardingResponse> onboard(
            @Valid @RequestBody OnboardingRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(employeeService.onboard(request));
    }

    // ── Get by ID

    /**
     * GET /api/admin/employees/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<OnboardingResponse> getEmployee(@PathVariable Long id) {
        return ResponseEntity.ok(employeeService.getEmployee(id));
    }

    // ── List All (NEW) ────────────────────────────────────────────────────────

    /**
     * GET /api/admin/employees
     * Query params:
     *   search  – partial name search (optional)
     *   dept    – department filter (optional)
     *   status  – ACTIVE | RESIGNED | TERMINATED (optional)
     *   page    – 0-indexed (default 0)
     *   size    – page size (default 10)
     *
     * Example:
     *   GET /api/admin/employees?search=arjun&dept=Tech&status=ACTIVE&page=0&size=10
     */
    @GetMapping
    public ResponseEntity<Page<EmployeeListResponse>> listEmployees(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String dept,
            @RequestParam(required = false) EmployeeStatus status,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "10") int size) {

        return ResponseEntity.ok(
                employeeManagementService.listEmployees(search, dept, status, page, size));
    }

    // Update Status

    /**
     * PATCH /api/admin/employees/{id}/status
     * Body: { "status": "RESIGNED" }
     */
    @PatchMapping("/{id}/status")
    public ResponseEntity<EmployeeListResponse> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody EmployeeStatusUpdateRequest request) {

        return ResponseEntity.ok(employeeManagementService.updateStatus(id, request));
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<EmployeeListResponse> updateEmployee(
            @PathVariable Long id,
            @Valid @RequestBody EmployeeUpdateRequest request) {

        return ResponseEntity.ok(
                employeeManagementService.updateEmployee(id, request)
        );
    }
    
    @GetMapping("/exited/pending-experience-letter")
    public ResponseEntity<Page<EmployeeListResponse>> getExitedEmployeesPendingExpLetter(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String department,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
 
        Page<EmployeeListResponse> result =
                employeeManagementService.listExitedEmployeesWithoutExpLetter(
                        search, department, page, size);
 
        return ResponseEntity.ok(result);
    }
}
