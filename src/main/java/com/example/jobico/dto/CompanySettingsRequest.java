package com.example.jobico.dto;

import jakarta.validation.constraints.NotBlank;

public class CompanySettingsRequest {

    @NotBlank(message = "Company name is required")
    private String name;

    private String website;
    private String industry;
    private String address;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getWebsite() { return website; }
    public void setWebsite(String website) { this.website = website; }

    public String getIndustry() { return industry; }
    public void setIndustry(String industry) { this.industry = industry; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
}
