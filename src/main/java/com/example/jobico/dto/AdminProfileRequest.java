package com.example.jobico.dto;

import jakarta.validation.constraints.NotBlank;

public class AdminProfileRequest {

    @NotBlank(message = "Name is required")
    private String name;

    private String phone;
    private String designation;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getDesignation() { return designation; }
    public void setDesignation(String designation) { this.designation = designation; }
}
