package com.example.jobico.controller;

import com.example.jobico.dto.*;
import com.example.jobico.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired 
    private AuthService authService;

    // ── USER: OTP Login

    @PostMapping("/send-otp")
    public ResponseEntity<ApiResponse<?>> sendOtp(@Valid @RequestBody OtpRequest request) {
        authService.sendOtp(request);

        return ResponseEntity.ok(
                new ApiResponse<>(
                        true,
                        "OTP sent successfully to +91" + request.getMobile(),
                        null,
                        HttpStatus.OK.value()
                )
        );
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<ApiResponse<AuthResponse>> verifyOtp(
            @Valid @RequestBody OtpVerifyRequest request) {

        AuthResponse response = authService.verifyOtpAndLogin(request);

        return ResponseEntity.ok(
                new ApiResponse<>(
                        true,
                        "Login successful",
                        response,
                        HttpStatus.OK.value()
                )
        );
    }

    // ── ADMIN: Register

    @PostMapping("/admin/register")
    public ResponseEntity<ApiResponse<AuthResponse>> adminRegister(
            @Valid @RequestBody AdminRegisterRequest request) {

        AuthResponse response = authService.adminRegister(request);

        return ResponseEntity.status(HttpStatus.CREATED).body(
                new ApiResponse<>(
                        true,
                        "Admin registered successfully",
                        response,
                        HttpStatus.CREATED.value()
                )
        );
    }

    // ── ADMIN: Login

    @PostMapping("/admin/login")
    public ResponseEntity<ApiResponse<AuthResponse>> adminLogin(
            @Valid @RequestBody AdminLoginRequest request) {

        AuthResponse response = authService.adminLogin(request);

        return ResponseEntity.ok(
                new ApiResponse<>(
                        true,
                        "Login successful",
                        response,
                        HttpStatus.OK.value()
                )
        );
    }

    // ── ADMIN: Forgot Password

    @PostMapping("/admin/forgot-password")
    public ResponseEntity<ApiResponse<?>> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequest request) {

        authService.forgotPassword(request);

        return ResponseEntity.ok(
                new ApiResponse<>(
                        true,
                        "Password reset link sent successfully",
                        null,
                        HttpStatus.OK.value()
                )
        );
    }

    // ── ADMIN: Reset Password

    @PutMapping("/admin/reset-password")
    public ResponseEntity<ApiResponse<?>> resetPassword(
            @Valid @RequestBody ResetPasswordRequest request) {

        authService.resetPassword(request);

        return ResponseEntity.ok(
                new ApiResponse<>(
                        true,
                        "Password reset successful",
                        null,
                        HttpStatus.OK.value()
                )
        );
    }

    @PutMapping("/admin/update-password")
    public ResponseEntity<ApiResponse<?>> updatePassword(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody UpdatePasswordRequest request) {

        authService.updatePassword(userDetails.getUsername(), request);

        return ResponseEntity.ok(
                new ApiResponse<>(
                        true,
                        "Password updated successfully",
                        null,
                        HttpStatus.OK.value()
                )
        );
    }
} 