package com.example.jobico.repository;

import com.example.jobico.entity.Payroll;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * UPDATED PayrollRepository — replace the existing one in your project.
 */
public interface PayrollRepository extends JpaRepository<Payroll, Long> {

    List<Payroll> findByEmployeeId(Long employeeId);

    Optional<Payroll> findByEmployeeIdAndMonthAndYear(Long employeeId, int month, int year);

    // ── NEW: Dashboard ────────────────────────────────────────────
    @Query("SELECT COALESCE(SUM(p.netSalary), 0) FROM Payroll p WHERE p.month = :month AND p.year = :year")
    double sumNetSalaryByMonthAndYear(@Param("month") int month, @Param("year") int year);
}
