package com.example.jobico.service;

import com.example.jobico.dto.*;
import com.example.jobico.entity.*;
import com.example.jobico.exception.ResourceNotFoundException;
import com.example.jobico.exception.UnauthorizedException;
import com.example.jobico.repository.*;
import com.example.jobico.security.JwtUtil;

import jakarta.transaction.Transactional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class AuthService {

    private static final Logger log =
            LoggerFactory.getLogger(AuthService.class);

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AdminRepository adminRepository;

    @Autowired
    private PasswordResetTokenRepository tokenRepository;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private OtpService otpService;

    @Autowired
    private SmsLoginService smsLoginService;

    @Autowired
    private EmailService emailService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private AdminProfileRepository adminProfileRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Value("${app.base-url:http://localhost:8081}")
    private String baseUrl;

    @Value("${app.admin-secret-key:JOBICO_ADMIN_2024}")
    private String adminSecretKey;

    // =========================================================================
    // USER : SEND OTP
    // =========================================================================

    public void sendOtp(OtpRequest request) {

        log.info(
                "AuthService: sending OTP mobile={}",
                request.getMobile()
        );

        // CHECK MOBILE REGISTERED OR NOT

        User user = userRepository
                .findByMobile(request.getMobile())
                .orElseThrow(() ->
                        new RuntimeException(
                                "Mobile number not registered."
                        )
                );

        // GENERATE OTP

        String otp = otpService.generateOtp(
                request.getMobile()
        );

        // SEND OTP SMS

        smsLoginService.sendOtp(
                request.getMobile(),
                otp
        );

        log.info(
                "AuthService: OTP sent successfully mobile={}",
                request.getMobile()
        );
    }

    // =========================================================================
    // USER : VERIFY OTP LOGIN
    // =========================================================================

    public AuthResponse verifyOtpAndLogin(
            OtpVerifyRequest request
    ) {

        // VERIFY OTP

        boolean valid = otpService.verifyOtp(
                request.getMobile(),
                request.getOtp()
        );

        if (!valid) {

            throw new RuntimeException(
                    "Invalid or expired OTP."
            );
        }

        // FETCH REGISTERED USER

        User user = userRepository
                .findByMobile(request.getMobile())
                .orElseThrow(() ->
                        new RuntimeException(
                                "User not found."
                        )
                );

        // CHECK FIRST LOGIN

        boolean isNew = user.isNewUser();

        if (user.isNewUser()) {

            user.setNewUser(false);

            userRepository.save(user);
        }

        // CHECK EMPLOYEE

        boolean isEmployee =
                employeeRepository
                        .findByUserMobile(
                                request.getMobile()
                        )
                        .isPresent();

        // GENERATE JWT TOKEN

        String token = jwtUtil.generateToken(
                user.getMobile(),
                user.getMobile(),
                user.getId(),
                user.getRole()
        );

        // RESPONSE

        AuthResponse response = new AuthResponse(
                token,
                user.getId(),
                null,
                user.getMobile(),
                user.getRole(),
                isNew
        );

        response.setEmployee(isEmployee);

        return response;
    }

    // =========================================================================
    // ADMIN : REGISTER
    // =========================================================================

    @Transactional
    public AuthResponse adminRegister(
            AdminRegisterRequest request
    ) {

        if (!adminSecretKey.equals(
                request.getAdminSecretKey()
        )) {

            throw new RuntimeException(
                    "Invalid admin secret key."
            );
        }

        if (adminRepository.existsByEmail(
                request.getEmail()
        )) {

            throw new RuntimeException(
                    "An admin with this email already exists."
            );
        }

        Admin admin = new Admin();

        admin.setEmail(request.getEmail());

        admin.setPassword(
                passwordEncoder.encode(
                        request.getPassword()
                )
        );

        adminRepository.save(admin);

        AdminProfile profile =
                adminProfileRepository
                        .findByAdmin(admin)
                        .orElse(null);

        String name =
                (profile != null
                        && profile.getName() != null)

                        ? profile.getName()

                        : admin.getEmail();

        String token = jwtUtil.generateToken(
                admin.getEmail(),
                name,
                admin.getId(),
                "ROLE_ADMIN"
        );

        return new AuthResponse(
                token,
                admin.getId(),
                admin.getEmail(),
                null,
                "ROLE_ADMIN",
                false
        );
    }

    // =========================================================================
    // ADMIN : LOGIN
    // =========================================================================

    public AuthResponse adminLogin(
            AdminLoginRequest request
    ) {

        Admin admin = adminRepository
                .findByEmail(request.getEmail())
                .orElseThrow(
                        () -> new RuntimeException(
                                "Invalid email or password."
                        )
                );

        if (!passwordEncoder.matches(
                request.getPassword(),
                admin.getPassword()
        )) {

            throw new UnauthorizedException(
                    "Invalid email or password."
            );
        }

        AdminProfile profile =
                adminProfileRepository
                        .findByAdmin(admin)
                        .orElse(null);

        String name =
                (profile != null
                        && profile.getName() != null)

                        ? profile.getName()

                        : admin.getEmail();

        String token = jwtUtil.generateToken(
                admin.getEmail(),
                name,
                admin.getId(),
                "ROLE_ADMIN"
        );

        return new AuthResponse(
                token,
                admin.getId(),
                admin.getEmail(),
                null,
                "ROLE_ADMIN",
                false
        );
    }

    // =========================================================================
    // ADMIN : FORGOT PASSWORD
    // =========================================================================

    @Transactional
    public String forgotPassword(
            ForgotPasswordRequest request
    ) {

        Admin admin = adminRepository
                .findByEmail(request.getEmail())
                .orElseThrow(
                        () -> new ResourceNotFoundException(
                                "No admin found with this email."
                        )
                );

        tokenRepository.deleteByAdminId(admin.getId());

        String rawToken = UUID.randomUUID().toString();

        PasswordResetToken resetToken =
                new PasswordResetToken();

        resetToken.setToken(rawToken);

        resetToken.setAdmin(admin);

        resetToken.setExpiresAt(
                LocalDateTime.now().plusMinutes(15)
        );

        resetToken.setUsed(false);

        tokenRepository.save(resetToken);

        String resetLink =
                baseUrl
                        + "/api/auth/admin/reset-password?token="
                        + rawToken;

        emailService.sendPasswordResetEmail(
                admin.getEmail(),
                resetLink
        );

        return "Password reset link sent to "
                + admin.getEmail();
    }

    // =========================================================================
    // ADMIN : RESET PASSWORD
    // =========================================================================

    @Transactional
    public String resetPassword(
            ResetPasswordRequest request
    ) {

        if (!request.getNewPassword().equals(
                request.getConfirmPassword()
        )) {

            throw new RuntimeException(
                    "Passwords do not match."
            );
        }

        PasswordResetToken resetToken =
                tokenRepository.findByToken(
                        request.getToken()
                ).orElseThrow(
                        () -> new RuntimeException(
                                "Invalid or expired token."
                        )
                );

        if (resetToken.isUsed()) {

            throw new RuntimeException(
                    "Token already used."
            );
        }

        if (resetToken.getExpiresAt()
                .isBefore(LocalDateTime.now())) {

            throw new RuntimeException(
                    "Token expired."
            );
        }

        Admin admin = resetToken.getAdmin();

        admin.setPassword(
                passwordEncoder.encode(
                        request.getNewPassword()
                )
        );

        adminRepository.save(admin);

        resetToken.setUsed(true);

        tokenRepository.save(resetToken);

        emailService.sendPasswordChangedEmail(
                admin.getEmail()
        );

        return "Password reset successful.";
    }

    // =========================================================================
    // ADMIN : UPDATE PASSWORD
    // =========================================================================

    @Transactional
    public String updatePassword(
            String email,
            UpdatePasswordRequest request
    ) {

        if (!request.getNewPassword().equals(
                request.getConfirmPassword()
        )) {

            throw new RuntimeException(
                    "Passwords do not match."
            );
        }

        Admin admin = adminRepository
                .findByEmail(email)
                .orElseThrow(
                        () -> new ResourceNotFoundException(
                                "Admin not found."
                        )
                );

        if (!passwordEncoder.matches(
                request.getCurrentPassword(),
                admin.getPassword()
        )) {

            throw new RuntimeException(
                    "Current password incorrect."
            );
        }

        if (passwordEncoder.matches(
                request.getNewPassword(),
                admin.getPassword()
        )) {

            throw new RuntimeException(
                    "New password must be different."
            );
        }

        admin.setPassword(
                passwordEncoder.encode(
                        request.getNewPassword()
                )
        );

        adminRepository.save(admin);

        emailService.sendPasswordChangedEmail(
                admin.getEmail()
        );

        return "Password updated successfully.";
    }
}