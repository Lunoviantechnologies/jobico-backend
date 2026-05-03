package com.example.jobico.repository;

import com.example.jobico.entity.ExperienceLetter;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ExperienceLetterRepository extends JpaRepository<ExperienceLetter, Long> {

    List<ExperienceLetter> findByEmployeeIdOrderByGeneratedAtDesc(Long employeeId);

    Optional<ExperienceLetter> findTopByEmployeeIdOrderByGeneratedAtDesc(Long employeeId);

    @Query("""
            SELECT el FROM ExperienceLetter el
            JOIN el.employee emp
            JOIN emp.candidate c
            WHERE (:search IS NULL
                OR LOWER(c.firstName)  LIKE LOWER(CONCAT('%',:search,'%'))
                OR LOWER(c.surname)    LIKE LOWER(CONCAT('%',:search,'%'))
                OR LOWER(el.referenceNumber) LIKE LOWER(CONCAT('%',:search,'%')))
            ORDER BY el.generatedAt DESC
            """)
    Page<ExperienceLetter> search(@Param("search") String search, Pageable pageable);

    Optional<String> findPdfUrlById(Long id);
}