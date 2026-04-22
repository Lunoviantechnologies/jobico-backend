package com.example.jobico.dto;

import com.example.jobico.entity.EmployeeStatus;
import jakarta.validation.constraints.NotNull;

public class EmployeeStatusUpdateRequest {

    @NotNull(message = "Status is required")
    private EmployeeStatus status;

    public EmployeeStatus getStatus() { return status; }
    public void setStatus(EmployeeStatus status) { this.status = status; }
}
