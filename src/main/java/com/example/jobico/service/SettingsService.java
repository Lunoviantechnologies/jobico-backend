package com.example.jobico.service;

import com.example.jobico.dto.*;
import com.example.jobico.entity.Admin;
import com.example.jobico.entity.AdminProfile;
import com.example.jobico.entity.CompanySettings;
import com.example.jobico.exception.ResourceNotFoundException;
import com.example.jobico.repository.AdminProfileRepository;
import com.example.jobico.repository.AdminRepository;
import com.example.jobico.repository.CompanySettingsRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SettingsService {

    @Autowired private AdminRepository adminRepository;
    @Autowired private AdminProfileRepository adminProfileRepository;
    @Autowired private CompanySettingsRepository companySettingsRepository;

    // ── Admin Profile ─────────────────────────────────────────────────────────

    public AdminProfileResponse getProfile(String email) {
        Admin admin = adminRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Admin not found"));

        AdminProfile profile = adminProfileRepository.findByAdminId(admin.getId())
                .orElseGet(() -> {
                    AdminProfile p = new AdminProfile();
                    p.setAdmin(admin);
                    p.setName("Super Admin");
                    p.setDesignation("Super Admin");
                    return p;
                });

        return toProfileResponse(admin, profile);
    }

    @Transactional
    public AdminProfileResponse updateProfile(String email, AdminProfileRequest request) {
        Admin admin = adminRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Admin not found"));

        AdminProfile profile = adminProfileRepository.findByAdminId(admin.getId())
                .orElseGet(() -> {
                    AdminProfile p = new AdminProfile();
                    p.setAdmin(admin);
                    return p;
                });

        profile.setName(request.getName());
        profile.setPhone(request.getPhone());
        profile.setDesignation(request.getDesignation());
        adminProfileRepository.save(profile);

        return toProfileResponse(admin, profile);
    }

    private AdminProfileResponse toProfileResponse(Admin admin, AdminProfile profile) {
        AdminProfileResponse r = new AdminProfileResponse();
        r.setId(admin.getId());
        r.setEmail(admin.getEmail());
        r.setName(profile.getName() != null ? profile.getName() : "Super Admin");
        r.setPhone(profile.getPhone());
        r.setDesignation(profile.getDesignation() != null ? profile.getDesignation() : "Super Admin");
        return r;
    }

    // ── Company Settings ──────────────────────────────────────────────────────

    /**
     * Always returns the single company-settings row (id = 1).
     * Creates it with defaults if it doesn't exist yet.
     */
    public CompanySettingsRequest getCompanySettings() {
        CompanySettings cs = companySettingsRepository.findById(1L)
                .orElseGet(this::defaultCompanySettings);
        return toCompanyDto(cs);
    }

    @Transactional
    public CompanySettingsRequest updateCompanySettings(CompanySettingsRequest request) {
        CompanySettings cs = companySettingsRepository.findById(1L)
                .orElseGet(this::defaultCompanySettings);

        cs.setName(request.getName());
        cs.setWebsite(request.getWebsite());
        cs.setIndustry(request.getIndustry());
        cs.setAddress(request.getAddress());

        return toCompanyDto(companySettingsRepository.save(cs));
    }

    private CompanySettings defaultCompanySettings() {
        CompanySettings cs = new CompanySettings();
        cs.setName("Jobico Technologies Pvt. Ltd.");
        cs.setWebsite("https://jobico.com");
        cs.setIndustry("Technology");
        cs.setAddress("Hyderabad, Telangana");
        return companySettingsRepository.save(cs);
    }

    private CompanySettingsRequest toCompanyDto(CompanySettings cs) {
        CompanySettingsRequest dto = new CompanySettingsRequest();
        dto.setName(cs.getName());
        dto.setWebsite(cs.getWebsite());
        dto.setIndustry(cs.getIndustry());
        dto.setAddress(cs.getAddress());
        return dto;
    }
}
