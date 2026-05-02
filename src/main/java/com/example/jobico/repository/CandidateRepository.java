package com.example.jobico.repository;

// ADD these methods to the existing CandidateRepository interface:

import com.example.jobico.entity.Candidate;
import com.example.jobico.entity.CandidateStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import java.util.Optional;

/**
 * UPDATED CandidateRepository — replace the existing one in your project.
 * Added: countByStatus, findByStatus (pageable), searchByNameAndStatus
 */
public interface CandidateRepository extends JpaRepository<Candidate, Long>, JpaSpecificationExecutor<Candidate> {

    Optional<Candidate> findByUser_Id(Long userId); // alternative user lookup

    Optional<Candidate> findByUser(com.example.jobico.entity.User user);

    @Query("SELECT c FROM Candidate c WHERE LOWER(CONCAT(c.firstName, ' ', c.surname)) LIKE LOWER(CONCAT('%', :name, '%'))")
    Page<Candidate> searchByName(@Param("name") String name, Pageable pageable);

    Page<Candidate> findByCategory(String category, Pageable pageable);

    Page<Candidate> findByRoleContainingIgnoreCase(String role, Pageable pageable);

    Page<Candidate> findByCategoryAndRoleContainingIgnoreCase(String category, String role, Pageable pageable);

    Page<Candidate> findByExperienceBetween(int minExp, int maxExp, Pageable pageable);

    @Query("SELECT DISTINCT c FROM Candidate c JOIN c.skills s WHERE LOWER(s.skillName) LIKE LOWER(CONCAT('%', :skill, '%'))")
    Page<Candidate> searchBySkill(@Param("skill") String skill, Pageable pageable);

    // ── NEW: Dashboard stats ──────────────────────────────────────
    long countByStatus(CandidateStatus status);

    Page<Candidate> findByStatus(CandidateStatus status, Pageable pageable);

    @Query("SELECT c FROM Candidate c WHERE LOWER(CONCAT(c.firstName, ' ', c.surname)) LIKE LOWER(CONCAT('%', :name, '%')) AND c.status = :status")
    Page<Candidate> searchByNameAndStatus(@Param("name") String name, @Param("status") CandidateStatus status, Pageable pageable);
    @Query("""
    	    SELECT c FROM Candidate c 
    	    WHERE LOWER(CONCAT(c.firstName, ' ', c.surname)) LIKE LOWER(CONCAT('%', :name, '%'))
    	       OR LOWER(c.firstName) LIKE LOWER(CONCAT('%', :name, '%'))
    	       OR LOWER(c.surname) LIKE LOWER(CONCAT('%', :name, '%'))
    	""")
    Page<Candidate> searchByFullName(@Param("name") String name, Pageable pageable);
}
