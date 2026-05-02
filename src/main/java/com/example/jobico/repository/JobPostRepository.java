package com.example.jobico.repository;

import com.example.jobico.entity.JobPost;
import com.example.jobico.entity.JobPostStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface JobPostRepository
        extends JpaRepository<JobPost, Long>, JpaSpecificationExecutor<JobPost> {

    // ─── Admin-scoped queries ─────────────────────────────────────────────────

    /** All posts belonging to a specific admin, newest first. */
    Page<JobPost> findByAdmin_IdOrderByCreatedAtDesc(Long adminId, Pageable pageable);

    /** Count active posts for a specific admin (used on dashboard). */
    long countByAdmin_IdAndStatus(Long adminId, JobPostStatus status);

    // ─── Status filter ────────────────────────────────────────────────────────

    Page<JobPost> findByStatusOrderByCreatedAtDesc(JobPostStatus status, Pageable pageable);

    // ─── Full-text title search (case-insensitive) ────────────────────────────

    @Query("""
            SELECT jp FROM JobPost jp
            WHERE LOWER(jp.title) LIKE LOWER(CONCAT('%', :keyword, '%'))
            ORDER BY jp.createdAt DESC
            """)
    Page<JobPost> searchByTitle(@Param("keyword") String keyword, Pageable pageable);
   
    @Query("""
            SELECT DISTINCT jp FROM JobPost jp
            JOIN jp.techStack ts
            WHERE LOWER(ts) LIKE LOWER(CONCAT('%', :skill, '%'))
            ORDER BY jp.createdAt DESC
            """)
    Page<JobPost> searchBySkill(@Param("skill") String skill, Pageable pageable);

    // ─── Dashboard aggregate ──────────────────────────────────────────────────

    /** Total job posts grouped by status for the dashboard stats card. */
    @Query("SELECT jp.status, COUNT(jp) FROM JobPost jp GROUP BY jp.status")
    java.util.List<Object[]> countGroupedByStatus();
}