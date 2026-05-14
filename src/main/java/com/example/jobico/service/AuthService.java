package com.example.jobico.service;

import com.example.jobico.dto.*;
import com.example.jobico.entity.*;
import com.example.jobico.exception.ResourceNotFoundException;
import com.example.jobico.exception.UnauthorizedException;
import com.example.jobico.repository.*;
import com.example.jobico.security.JwtUtil;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * FULL REPLACEMENT for AuthService.
 * Adds: adminRegister() method.
 */
@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    @Autowired private UserRepository userRepository;
    @Autowired private AdminRepository adminRepository;
    @Autowired private PasswordResetTokenRepository tokenRepository;
    @Autowired private JwtUtil jwtUtil;
    @Autowired private OtpService otpService;
    @Autowired private EmailService emailService;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private AdminProfileRepository adminProfileRepository;
    @Autowired private EmployeeRepository employeeRepository;

    @Value("${app.base-url:http://localhost:8081}")
    private String baseUrl;

    /**
     * Secret key required when registering a new admin account.
     * Set in application.properties: app.admin-secret-key=JOBICO_ADMIN_2024
     */
    @Value("${app.admin-secret-key:JOBICO_ADMIN_2024}")
    private String adminSecretKey;

    /**
     * Creates a default admin (admin@jobico.com / admin@123) on first run
     * if no admin exists yet. Safe to leave in production — only runs once.
     */
//    @PostConstruct
//    public void seedDefaultAdmin() {
//        if (!adminRepository.existsByEmail("admin@jobico.com")) {
//            Admin admin = new Admin();
//            admin.setEmail("admin@jobico.com");
//            admin.setPassword(passwordEncoder.encode("admin@123"));
//            adminRepository.save(admin);
//            System.out.println("[Jobico] Default admin seeded: admin@jobico.com / admin@123");
//        }
//    }

    // ── USER: OTP Login ───────────────────────────────────────────────────────

    public void sendOtp(OtpRequest request) {
        log.info("AuthService: sending OTP mobile={}", request.getMobile());
        otpService.generateOtp(request.getMobile());
        log.info("AuthService: OTP generated mobile={}", request.getMobile());
    }

    public AuthResponse verifyOtpAndLogin(OtpVerifyRequest request) {
        boolean valid = otpService.verifyOtp(request.getMobile(), request.getOtp());
        if (!valid) throw new RuntimeException("Invalid or expired OTP.");

        boolean isNew = !userRepository.existsByMobile(request.getMobile());
        User user;

        if (isNew) {
            user = new User();
            user.setMobile(request.getMobile());
            user.setRole("ROLE_USER");
            user.setNewUser(true);
            userRepository.save(user);
        } else {
            user = userRepository.findByMobile(request.getMobile())
                    .orElseThrow(() -> new RuntimeException("User not found"));
            if (user.isNewUser()) {
                user.setNewUser(false);
                userRepository.save(user);
            }
        }

        // ✅ Check if this user is an onboarded employee
        boolean isEmployee = employeeRepository.findByUserMobile(request.getMobile()).isPresent();

        String token = jwtUtil.generateToken(user.getMobile(), user.getMobile(), user.getId(), user.getRole());
        AuthResponse response = new AuthResponse(token, user.getId(), null, user.getMobile(), user.getRole(), isNew);
        response.setEmployee(isEmployee);  
        return response;
    }

    // ── ADMIN: Register (NEW) 

    @Transactional
    public AuthResponse adminRegister(AdminRegisterRequest request) {

        if (!adminSecretKey.equals(request.getAdminSecretKey()))
            throw new RuntimeException("Invalid admin secret key.");

        if (adminRepository.existsByEmail(request.getEmail()))
            throw new RuntimeException("An admin with this email already exists.");

        Admin admin = new Admin();
        admin.setEmail(request.getEmail());
        admin.setPassword(passwordEncoder.encode(request.getPassword()));
        adminRepository.save(admin);

        AdminProfile profile = adminProfileRepository.findByAdmin(admin).orElse(null);

        String name = (profile != null && profile.getName() != null) ? profile.getName() : admin.getEmail();

        String token = jwtUtil.generateToken( admin.getEmail(), name,admin.getId(), "ROLE_ADMIN");
        return new AuthResponse(token,admin.getId(),admin.getEmail(), null,"ROLE_ADMIN",false);
    }

    // ── ADMIN: Login 
    public AuthResponse adminLogin(AdminLoginRequest request) {
        Admin admin = adminRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("Invalid email or password."));

        if (!passwordEncoder.matches(request.getPassword(), admin.getPassword()))
            throw new UnauthorizedException("Invalid email or password.");
        AdminProfile profile = adminProfileRepository.findByAdmin(admin).orElse(null);

        String name = (profile != null && profile.getName() != null) ? profile.getName() : admin.getEmail();

        String token = jwtUtil.generateToken(admin.getEmail(),name,admin.getId(), "ROLE_ADMIN");

        return new AuthResponse(token, admin.getId(), admin.getEmail(), null, "ROLE_ADMIN", false);
    }

    // ADMIN: Forgot Password
    @Transactional
    public String forgotPassword(ForgotPasswordRequest request) {
        Admin admin = adminRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("No admin found with this email."));

        tokenRepository.deleteByAdminId(admin.getId());

        String rawToken = UUID.randomUUID().toString();
        PasswordResetToken resetToken = new PasswordResetToken();
        resetToken.setToken(rawToken);
        resetToken.setAdmin(admin);
        resetToken.setExpiresAt(LocalDateTime.now().plusMinutes(15));
        resetToken.setUsed(false);
        tokenRepository.save(resetToken);

        String resetLink = baseUrl + "/api/auth/admin/reset-password?token=" + rawToken;
        emailService.sendPasswordResetEmail(admin.getEmail(), resetLink);

        return "Password reset link sent to " + admin.getEmail();
    }

  
    @Transactional
    public String resetPassword(ResetPasswordRequest request) {
        if (!request.getNewPassword().equals(request.getConfirmPassword()))
            throw new RuntimeException("Passwords do not match.");

        PasswordResetToken resetToken = tokenRepository.findByToken(request.getToken())
                .orElseThrow(() -> new RuntimeException("Invalid or expired token."));

        if (resetToken.isUsed())
            throw new RuntimeException("Token already used.");
        if (resetToken.getExpiresAt().isBefore(LocalDateTime.now()))
            throw new RuntimeException("Token expired.");

        Admin admin = resetToken.getAdmin();
        admin.setPassword(passwordEncoder.encode(request.getNewPassword()));
        adminRepository.save(admin);

        resetToken.setUsed(true);
        tokenRepository.save(resetToken);

        emailService.sendPasswordChangedEmail(admin.getEmail());
        return "Password reset successful.";
    }

    // ── ADMIN: Update Password ────────────────────────────────────────────────

    @Transactional
    public String updatePassword(String email, UpdatePasswordRequest request) {
        if (!request.getNewPassword().equals(request.getConfirmPassword()))
            throw new RuntimeException("Passwords do not match.");

        Admin admin = adminRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Admin not found."));

        if (!passwordEncoder.matches(request.getCurrentPassword(), admin.getPassword()))
            throw new RuntimeException("Current password incorrect.");

        if (passwordEncoder.matches(request.getNewPassword(), admin.getPassword()))
            throw new RuntimeException("New password must be different.");

        admin.setPassword(passwordEncoder.encode(request.getNewPassword()));
        adminRepository.save(admin);

        emailService.sendPasswordChangedEmail(admin.getEmail());
        return "Password updated successfully.";
    }
}
