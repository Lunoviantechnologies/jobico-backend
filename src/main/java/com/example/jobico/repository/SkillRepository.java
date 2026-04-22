package com.example.jobico.repository;

import com.example.jobico.entity.Skill;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SkillRepository extends JpaRepository<Skill, Long> {
    List<Skill> findByCandidateId(Long candidateId);
    void deleteByCandidateId(Long candidateId);
}