package com.example.jobico.repository;

import com.example.jobico.entity.PasswordResetToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {
    Optional<PasswordResetToken> findByToken(String token);
    void deleteByAdminId(Long adminId);
    void deleteByUserId(Long userId);
}