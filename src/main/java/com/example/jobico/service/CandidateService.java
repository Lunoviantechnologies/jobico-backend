package com.example.jobico.service;

import com.example.jobico.dto.*;
import com.example.jobico.entity.*;
import com.example.jobico.exception.ResourceNotFoundException;
import com.example.jobico.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.Period;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class CandidateService {

    @Autowired private CandidateRepository candidateRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private EmailService emailService;

    // ─── Create Profile ──────────────────────────────────────────────────────

    @Transactional
    public CandidateResponse createProfile(String mobile, CandidateRequest request) {
        User user = userRepository.findByMobile(mobile)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (candidateRepository.findByUser(user).isPresent())
            throw new RuntimeException("Profile already exists for this user");

        Candidate candidate = mapRequestToEntity(new Candidate(), request);
        candidate.setUser(user);
        candidate.setStatus(CandidateStatus.APPLIED);

        CandidateResponse response = mapEntityToResponse(candidateRepository.save(candidate));

        // Send application-received confirmation email
        if (candidate.getEmail() != null && !candidate.getEmail().isBlank()) {
            emailService.sendApplicationReceivedEmail(
                    candidate.getEmail(),
                    candidate.getFirstName() + " " + candidate.getSurname(),
                    candidate.getRole());
        }

        return response;
    }

    // ─── Get My Profile ──────────────────────────────────────────────────────

    public CandidateResponse getMyProfile(String mobile) {
        User user = userRepository.findByMobile(mobile)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        Candidate candidate = candidateRepository.findByUser(user)
                .orElseThrow(() -> new ResourceNotFoundException("Profile not found"));
        return mapEntityToResponse(candidate);
    }

    // ─── Update Profile ──────────────────────────────────────────────────────

    @Transactional
    public CandidateResponse updateProfile(String mobile, CandidateRequest request) {
        User user = userRepository.findByMobile(mobile)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        Candidate candidate = candidateRepository.findByUser(user)
                .orElseThrow(() -> new ResourceNotFoundException("Profile not found"));

        candidate.getEducationList().clear();
        candidate.getSkills().clear();
        mapRequestToEntity(candidate, request);
        return mapEntityToResponse(candidateRepository.save(candidate));
    }

    // ─── Delete Profile ──────────────────────────────────────────────────────

    @Transactional
    public void deleteProfile(String mobile) {
        User user = userRepository.findByMobile(mobile)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        Candidate candidate = candidateRepository.findByUser(user)
                .orElseThrow(() -> new ResourceNotFoundException("Profile not found"));
        candidateRepository.delete(candidate);
    }

    // ─── Get By ID (admin) ───────────────────────────────────────────────────

    public CandidateResponse getById(Long id) {
        return mapEntityToResponse(candidateRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Candidate not found: " + id)));
    }

    // ─── Get All Paginated (admin) ───────────────────────────────────────────

    public Page<CandidateResponse> getAll(int page, int size) {
        return candidateRepository.findAll(PageRequest.of(page, size, Sort.by("id").descending()))
                .map(this::mapEntityToResponse);
    }

    // ─── Search & Filter (admin) ─────────────────────────────────────────────

    public Page<CandidateResponse> searchByName(String name, int page, int size) {
        return candidateRepository.searchByName(name, PageRequest.of(page, size))
                .map(this::mapEntityToResponse);
    }

    public Page<CandidateResponse> filterByCategory(String category, int page, int size) {
        return candidateRepository.findByCategory(category, PageRequest.of(page, size))
                .map(this::mapEntityToResponse);
    }

    public Page<CandidateResponse> filterByRole(String role, int page, int size) {
        return candidateRepository.findByRoleContainingIgnoreCase(role, PageRequest.of(page, size))
                .map(this::mapEntityToResponse);
    }

    public Page<CandidateResponse> filterByCategoryAndRole(String category, String role, int page, int size) {
        return candidateRepository.findByCategoryAndRoleContainingIgnoreCase(category, role, PageRequest.of(page, size))
                .map(this::mapEntityToResponse);
    }

    public Page<CandidateResponse> searchBySkill(String skill, int page, int size) {
        return candidateRepository.searchBySkill(skill, PageRequest.of(page, size))
                .map(this::mapEntityToResponse);
    }

    public Page<CandidateResponse> filterByExperience(int min, int max, int page, int size) {
        return candidateRepository.findByExperienceBetween(min, max, PageRequest.of(page, size))
                .map(this::mapEntityToResponse);
    }

    // ─── Update Status (admin) ───────────────────────────────────────────────

    @Transactional
    public CandidateResponse updateStatus(Long candidateId, CandidateStatus newStatus) {
        Candidate candidate = candidateRepository.findById(candidateId)
                .orElseThrow(() -> new ResourceNotFoundException("Candidate not found: " + candidateId));
        candidate.setStatus(newStatus);
        CandidateResponse response = mapEntityToResponse(candidateRepository.save(candidate));

        // Send status-change email if candidate has an email
        String email = candidate.getEmail();
        String name  = candidate.getFirstName() + " " + candidate.getSurname();
        String role  = candidate.getRole();

        if (email != null && !email.isBlank()) {
            switch (newStatus) {
                case SHORTLISTED -> emailService.sendShortlistedEmail(email, name, role);
                case SELECTED    -> emailService.sendSelectedEmail(email, name, role);
                case REJECTED    -> emailService.sendRejectedEmail(email, name, role);
                default          -> { /* APPLIED — no additional email */ }
            }
        }

        return response;
    }

    // ─── Resume ──────────────────────────────────────────────────────────────

    @Transactional
    public void updateResumePath(String mobile, String resumePath) {
        User user = userRepository.findByMobile(mobile)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        Candidate candidate = candidateRepository.findByUser(user)
                .orElseThrow(() -> new ResourceNotFoundException("Profile not found. Create your profile first."));
        candidate.setResumePath(resumePath);
        candidateRepository.save(candidate);
    }

    public String getResumePath(Long candidateId) {
        Candidate candidate = candidateRepository.findById(candidateId)
                .orElseThrow(() -> new ResourceNotFoundException("Candidate not found"));
        if (candidate.getResumePath() == null || candidate.getResumePath().isBlank())
            throw new ResourceNotFoundException("No resume uploaded for this candidate");
        return candidate.getResumePath();
    }

    // ─── Mapper: Request → Entity ─────────────────────────────────────────────

    private Candidate mapRequestToEntity(Candidate candidate, CandidateRequest request) {
        candidate.setFirstName(request.getFirstName());
        candidate.setSurname(request.getSurname());
        candidate.setDob(request.getDob());
        candidate.setAge(calculateAge(request.getDob()));
        candidate.setEmail(request.getEmail());
        candidate.setCategory(request.getCategory());
        candidate.setRole(request.getRole());
        candidate.setExperience(request.getExperience());
        candidate.setWorkType(request.getWorkType());

        if (request.getEducationList() != null) {
            List<Education> educationList = request.getEducationList().stream().map(dto -> {
                Education edu = new Education();
                edu.setLevel(dto.getLevel());
                edu.setYear(dto.getYear());
                edu.setCollege(dto.getCollege());
                edu.setPercentage(dto.getPercentage());
                edu.setCandidate(candidate);
                return edu;
            }).collect(Collectors.toList());
            candidate.getEducationList().addAll(educationList);
        }

        if (request.getSkills() != null) {
            List<Skill> skills = request.getSkills().stream().map(dto -> {
                Skill skill = new Skill();
                skill.setSkillName(dto.getSkillName());
                skill.setExperience(dto.getExperience());
                skill.setCandidate(candidate);
                return skill;
            }).collect(Collectors.toList());
            candidate.getSkills().addAll(skills);
        }

        return candidate;
    }

    // ─── Mapper: Entity → Response ───────────────────────────────────────────

    public CandidateResponse mapEntityToResponse(Candidate candidate) {
        CandidateResponse response = new CandidateResponse();
        response.setId(candidate.getId());
        response.setFirstName(candidate.getFirstName());
        response.setSurname(candidate.getSurname());
        response.setDob(candidate.getDob());
        response.setAge(candidate.getAge());
        response.setCategory(candidate.getCategory());
        response.setRole(candidate.getRole());
        response.setExperience(candidate.getExperience());
        response.setWorkType(candidate.getWorkType());
        response.setResumePath(candidate.getResumePath());
        response.setMobile(candidate.getUser().getMobile());
        response.setEmail(candidate.getEmail());
        response.setStatus(candidate.getStatus());

        if (candidate.getEducationList() != null) {
            response.setEducationList(candidate.getEducationList().stream().map(edu -> {
                EducationDTO dto = new EducationDTO();
                dto.setId(edu.getId());
                dto.setLevel(edu.getLevel());
                dto.setYear(edu.getYear());
                dto.setCollege(edu.getCollege());
                dto.setPercentage(edu.getPercentage());
                return dto;
            }).collect(Collectors.toList()));
        }

        if (candidate.getSkills() != null) {
            response.setSkills(candidate.getSkills().stream().map(s -> {
                SkillDTO dto = new SkillDTO();
                dto.setId(s.getId());
                dto.setSkillName(s.getSkillName());
                dto.setExperience(s.getExperience());
                return dto;
            }).collect(Collectors.toList()));
        }

        return response;
    }

    private int calculateAge(LocalDate dob) {
        if (dob == null) return 0;
        return Period.between(dob, LocalDate.now()).getYears();
    }
    public Page<CandidateResponse> getCandidates(
        String search,
        String status,
        String category,
        int page,
        int size) {

    Pageable pageable = PageRequest.of(page, size, Sort.by("id").descending());
    Specification<Candidate> spec = Specification.where(null);

    //  SEARCH (name + email + role)
    if (search != null && !search.isBlank()) {
        spec = spec.and((root, query, cb) -> cb.or(
                cb.like(cb.lower(root.get("firstName")), "%" + search.toLowerCase() + "%"),
                cb.like(cb.lower(root.get("surname")), "%" + search.toLowerCase() + "%"),
                cb.like(cb.lower(root.get("email")), "%" + search.toLowerCase() + "%"),
                cb.like(cb.lower(root.get("role")), "%" + search.toLowerCase() + "%")
        ));
    }

    // STATUS FILTER
    if (status != null && !status.isBlank()) {
        spec = spec.and((root, query, cb) ->
                cb.equal(root.get("status"), CandidateStatus.valueOf(status))
        );
    }

    // CATEGORY FILTER
    if (category != null && !category.isBlank()) {
        spec = spec.and((root, query, cb) ->
                cb.equal(root.get("category"), category)
        );
    }

    return candidateRepository.findAll(spec, pageable)
            .map(this::mapEntityToResponse);
    }
}