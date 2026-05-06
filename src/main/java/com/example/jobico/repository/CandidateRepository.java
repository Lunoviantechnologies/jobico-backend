package com.example.jobico.repository;

import com.example.jobico.entity.Candidate;
import com.example.jobico.entity.CandidateStatus;
import com.example.jobico.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CandidateRepository extends JpaRepository<Candidate, Long>, JpaSpecificationExecutor<Candidate> {

    Optional<Candidate> findByUser_Id(Long userId);

    Optional<Candidate> findByUser(User user);

    // ── No @EntityGraph here — collections are loaded separately via entity's
    //    FetchType.LAZY + @BatchSize in the entity (see Candidate.java fix).
    //    Using @EntityGraph with two List collections + pagination causes
    //    MultipleBagFetchException and in-memory pagination warnings.
    Page<Candidate> findAll(Pageable pageable);

    Page<Candidate> findAll(Specification<Candidate> spec, Pageable pageable);

    @Query("SELECT c FROM Candidate c WHERE LOWER(CONCAT(c.firstName, ' ', c.surname)) LIKE LOWER(CONCAT('%', :name, '%'))")
    Page<Candidate> searchByName(@Param("name") String name, Pageable pageable);

    Page<Candidate> findByCategory(String category, Pageable pageable);

    Page<Candidate> findByRoleContainingIgnoreCase(String role, Pageable pageable);

    Page<Candidate> findByCategoryAndRoleContainingIgnoreCase(String category, String role, Pageable pageable);

    Page<Candidate> findByExperienceBetween(int minExp, int maxExp, Pageable pageable);

    @Query("SELECT DISTINCT c FROM Candidate c JOIN c.skills s WHERE LOWER(s.skillName) LIKE LOWER(CONCAT('%', :skill, '%'))")
    Page<Candidate> searchBySkill(@Param("skill") String skill, Pageable pageable);

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

    // Used internally to batch-load collections after pagination
    @Query("SELECT c FROM Candidate c LEFT JOIN FETCH c.educationList WHERE c.id IN :ids")
    List<Candidate> fetchEducationByIds(@Param("ids") List<Long> ids);

    @Query("SELECT c FROM Candidate c LEFT JOIN FETCH c.skills WHERE c.id IN :ids")
    List<Candidate> fetchSkillsByIds(@Param("ids") List<Long> ids);
}