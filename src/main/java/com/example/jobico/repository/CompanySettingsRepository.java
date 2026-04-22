package com.example.jobico.repository;

import com.example.jobico.entity.CompanySettings;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CompanySettingsRepository extends JpaRepository<CompanySettings, Long> {
    // Singleton row: always fetch/save id=1
}
