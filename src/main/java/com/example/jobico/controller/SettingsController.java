package com.example.jobico.controller;

import com.example.jobico.dto.*;
import com.example.jobico.service.SettingsService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

/**
 * Settings endpoints consumed by the React Settings page.
 *
 * Profile:
 *   GET  /api/admin/settings/profile
 *   PUT  /api/admin/settings/profile
 *
 * Company:
 *   GET  /api/admin/settings/company
 *   PUT  /api/admin/settings/company
 */
@RestController
@RequestMapping("/api/admin/settings")
public class SettingsController {

    @Autowired private SettingsService settingsService;

    // ── Admin Profile

    @GetMapping("/profile")
    public ResponseEntity<AdminProfileResponse> getProfile(
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(settingsService.getProfile(userDetails.getUsername()));
    }

    @PutMapping("/profile")
    public ResponseEntity<AdminProfileResponse> updateProfile(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody AdminProfileRequest request) {
        return ResponseEntity.ok(settingsService.updateProfile(userDetails.getUsername(), request));
    }

    // ── Company Settings ──────────────────────────────────────────────────────

    @GetMapping("/company")
    public ResponseEntity<CompanySettingsRequest> getCompanySettings() {
        return ResponseEntity.ok(settingsService.getCompanySettings());
    }

    @PutMapping("/company")
    public ResponseEntity<CompanySettingsRequest> updateCompanySettings(
            @Valid @RequestBody CompanySettingsRequest request) {
        return ResponseEntity.ok(settingsService.updateCompanySettings(request));
    }
}
