package com.example.jobico.controller;

import com.example.jobico.dto.*;
import com.example.jobico.service.CandidateService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/candidates")
public class CandidateController {

    @Autowired private CandidateService candidateService;

    /**
     * POST /api/candidates/profile
     * Create profile after OTP login (new user onboarding step).
     */
    @PostMapping("/profile")
    public ResponseEntity<CandidateResponse> createProfile(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody CandidateRequest request) {

        CandidateResponse response = candidateService.createProfile(userDetails.getUsername(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * GET /api/candidates/profile
     * Get the logged-in user's own profile.
     */
    @GetMapping("/profile")
    public ResponseEntity<CandidateResponse> getMyProfile(
            @AuthenticationPrincipal UserDetails userDetails) {

        return ResponseEntity.ok(candidateService.getMyProfile(userDetails.getUsername()));
    }

    /**
     * PUT /api/candidates/profile/update
     * Update the logged-in user's profile.
     */
    @PutMapping("/profile/update")
    public ResponseEntity<CandidateResponse> updateProfile(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody CandidateRequest request) {

        return ResponseEntity.ok(candidateService.updateProfile(userDetails.getUsername(), request));
    }

    /**
     * DELETE /api/candidates/profile/delete
     */
    @DeleteMapping("/profile/delete")
    public ResponseEntity<String> deleteProfile(
            @AuthenticationPrincipal UserDetails userDetails) {

        candidateService.deleteProfile(userDetails.getUsername());
        return ResponseEntity.ok("Profile deleted successfully");
    }
}