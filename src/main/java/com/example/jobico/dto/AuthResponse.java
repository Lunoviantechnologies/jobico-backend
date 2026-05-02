package com.example.jobico.dto;

public class AuthResponse {

    private String token;
    private Long id;
    private String email;
    private String mobile;
    private String role;
    private boolean newUser;
    private boolean employee;

    public AuthResponse() {}

    public AuthResponse(String token, Long id, String email, String mobile, String role, boolean newUser) {
        this.token = token;
        this.id = id;
        this.email = email;
        this.mobile = mobile;
        this.role = role;
        this.newUser = newUser;
        this.employee=false;
    }

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getMobile() { return mobile; }
    public void setMobile(String mobile) { this.mobile = mobile; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public boolean isNewUser() { return newUser; }
    public void setNewUser(boolean newUser) { this.newUser = newUser; }
    public boolean isEmployee() { return employee; }
    public void setEmployee(boolean employee) { this.employee = employee; }
}