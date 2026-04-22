package com.example.jobico.service;

import com.example.jobico.dto.*;
import com.example.jobico.entity.*;
import com.example.jobico.exception.ResourceNotFoundException;
import com.example.jobico.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class PayrollService {

    @Autowired private PayrollRepository payrollRepository;
    @Autowired private EmployeeRepository employeeRepository;

    @Transactional
    public PayrollResponse runPayroll(PayrollRequest request) {
        Employee emp = employeeRepository.findById(request.getEmployeeId())
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found"));

        if (emp.getEmployeeStatus() != EmployeeStatus.ACTIVE)
            throw new RuntimeException("Payroll can only be run for ACTIVE employees.");

        if (payrollRepository.findByEmployeeIdAndMonthAndYear(
                emp.getId(), request.getMonth(), request.getYear()).isPresent())
            throw new RuntimeException("Payroll already processed for this employee for " +
                    request.getMonth() + "/" + request.getYear());

        double net = request.getBasicSalary() + request.getHra()
                   + request.getAllowances() - request.getDeductions();

        Payroll p = new Payroll();
        p.setEmployee(emp);
        p.setMonth(request.getMonth());
        p.setYear(request.getYear());
        p.setBasicSalary(request.getBasicSalary());
        p.setHra(request.getHra());
        p.setAllowances(request.getAllowances());
        p.setDeductions(request.getDeductions());
        p.setNetSalary(net);
        p.setPaymentStatus(PaymentStatus.PAID);
        p.setPaymentDate(LocalDate.now());

        return toResponse(payrollRepository.save(p));
    }

    public List<PayrollResponse> getPayslips(Long employeeId) {
        if (!employeeRepository.existsById(employeeId))
            throw new ResourceNotFoundException("Employee not found");
        return payrollRepository.findByEmployeeId(employeeId)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

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