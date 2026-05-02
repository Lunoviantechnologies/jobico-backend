package com.example.jobico.service;

import com.example.jobico.dto.EmployeeListResponse;
import com.example.jobico.dto.EmployeeStatusUpdateRequest;
import com.example.jobico.entity.Employee;
import com.example.jobico.entity.EmployeeStatus;
import com.example.jobico.exception.ResourceNotFoundException;
import com.example.jobico.repository.EmployeeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EmployeeManagementService {

    @Autowired private EmployeeRepository employeeRepository;

    /**
     * List all employees with optional search + filter.
     *
     * @param search    partial name match (nullable)
     * @param dept      department filter (nullable)
     * @param status    EmployeeStatus filter (nullable)
     * @param page      0-indexed page number
     * @param size      page size
     */
    public Page<EmployeeListResponse> listEmployees(String search, String dept,
                                                     EmployeeStatus status,
                                                     int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("id").descending());

        Page<Employee> result;

        boolean hasSearch = search != null && !search.isBlank();
        boolean hasDept   = dept != null && !dept.isBlank();
        boolean hasStatus = status != null;

        if (hasSearch && hasDept) {
            result = employeeRepository.searchByNameAndDepartment(search, dept, pageable);
        } else if (hasSearch) {
            result = employeeRepository.searchByName(search, pageable);
        } else if (hasDept && hasStatus) {
            result = employeeRepository.findByDepartmentAndEmployeeStatus(dept, status, pageable);
        } else if (hasDept) {
            result = employeeRepository.findByDepartment(dept, pageable);
        } else if (hasStatus) {
            result = employeeRepository.findByEmployeeStatus(status, pageable);
        } else {
            result = employeeRepository.findAll(pageable);
        }

        return result.map(this::toListResponse);
    }

    /**
     * Update employee status (ACTIVE / RESIGNED / TERMINATED).
     */
    @Transactional
    public EmployeeListResponse updateStatus(Long employeeId, EmployeeStatusUpdateRequest request) {
        Employee emp = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found: " + employeeId));
        emp.setEmployeeStatus(request.getStatus());
        return toListResponse(employeeRepository.save(emp));
    }

    // ── Mapper ────────────────────────────────────────────────────────────────

    private EmployeeListResponse toListResponse(Employee emp) {
        EmployeeListResponse r = new EmployeeListResponse();
        r.setId(emp.getId());
        r.setEmployeeId(emp.getEmployeeId());
        r.setName(emp.getCandidate().getFirstName() + " " + emp.getCandidate().getSurname());
        r.setRole(emp.getCandidate().getRole());
        r.setDepartment(emp.getDepartment());
        r.setJoiningDate(emp.getJoiningDate());
        r.setStatus(emp.getEmployeeStatus());
        r.setMobile(emp.getCandidate().getUser().getMobile());
        r.setEmail(emp.getCandidate().getEmail());
        r.setSalary(emp.getSalary());

        // Mask bank account: show last 4 digits only, e.g. "HDFC ****4521"
        String bankName = emp.getBankName();
        String acct     = emp.getBankAccountNumber();
        if (bankName != null && acct != null && acct.length() >= 4) {
            String masked = bankName + " ****" + acct.substring(acct.length() - 4);
            r.setBankName(bankName);
            r.setBankAccountMasked(masked);
        } else {
            r.setBankName(bankName);
            r.setBankAccountMasked("—");
        }

        return r;
    }
}
