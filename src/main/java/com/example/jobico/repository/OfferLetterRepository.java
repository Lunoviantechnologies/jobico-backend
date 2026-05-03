package com.example.jobico.repository;

import com.example.jobico.entity.OfferLetter;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OfferLetterRepository extends JpaRepository<OfferLetter, Long> {

    List<OfferLetter> findByCandidateIdOrderByGeneratedAtDesc(Long candidateId);

    Optional<OfferLetter> findTopByCandidateIdOrderByGeneratedAtDesc(Long candidateId);

    /** Paginated admin dashboard list with optional name search */
    @Query("""
            SELECT ol FROM OfferLetter ol
            JOIN ol.candidate c
            WHERE (:search IS NULL
                OR LOWER(c.firstName) LIKE LOWER(CONCAT('%',:search,'%'))
                OR LOWER(c.surname)   LIKE LOWER(CONCAT('%',:search,'%'))
                OR LOWER(ol.referenceNumber) LIKE LOWER(CONCAT('%',:search,'%')))
            ORDER BY ol.generatedAt DESC
            """)
    Page<OfferLetter> search(@Param("search") String search, Pageable pageable);

    boolean existsByCandidateId(Long candidateId);

    Optional<String> findPdfUrlById(Long id);
}