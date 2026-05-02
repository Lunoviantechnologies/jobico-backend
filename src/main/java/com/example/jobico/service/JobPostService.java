package com.example.jobico.service;

import com.example.jobico.dto.JobPostRequest;
import com.example.jobico.dto.JobPostResponse;
import com.example.jobico.entity.Admin;
import com.example.jobico.entity.JobPost;
import com.example.jobico.entity.JobPostStatus;
import com.example.jobico.exception.ResourceNotFoundException;
import com.example.jobico.exception.UnauthorizedException;
import com.example.jobico.repository.AdminRepository;
import com.example.jobico.repository.JobPostRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@Service
public class JobPostService {

    private static final Logger log = LoggerFactory.getLogger(JobPostService.class);

    @Autowired private JobPostRepository jobPostRepository;
    @Autowired private AdminRepository   adminRepository;

  //create
    @Transactional
    public JobPostResponse create(String adminEmail, JobPostRequest request) {
        Admin admin = resolveAdmin(adminEmail);
        validateSalaryRange(request);

        JobPost jobPost = mapRequestToEntity(new JobPost(), request);
        jobPost.setAdmin(admin);

        JobPost saved = jobPostRepository.save(jobPost);
        log.info("JobPost created: id={} title='{}' by admin={}", saved.getId(), saved.getTitle(), adminEmail);
        return mapEntityToResponse(saved);
    }

    // ─── Get All (with dynamic search + filter) 

   
    @Transactional(readOnly = true)
    public Page<JobPostResponse> getAll(
            String adminEmail,
            String keyword,
            String category,
            JobPostStatus status,
            String jobType,
            String workType,
            int page,
            int size) {

        Admin admin = resolveAdmin(adminEmail);

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());

        Specification<JobPost> spec = Specification
                // always scope to this admin's posts
                .where(adminEquals(admin.getId()));

        if (keyword  != null && !keyword.isBlank())
            spec = spec.and(titleContains(keyword));

        if (category != null && !category.isBlank())
            spec = spec.and(categoryEquals(category));

        if (status   != null)
            spec = spec.and(statusEquals(status));

        if (jobType  != null && !jobType.isBlank())
            spec = spec.and(jobTypeEquals(jobType));

        if (workType != null && !workType.isBlank())
            spec = spec.and(workTypeEquals(workType));

        return jobPostRepository.findAll(spec, pageable)
                .map(this::mapEntityToResponse);
    }

    // ─── Get Single

    /**
     * Returns a single job post.
     * Admins can only view their own posts; ownership is enforced here.
     */
    @Transactional(readOnly = true)
    public JobPostResponse getById(String adminEmail, Long jobPostId) {
        JobPost jobPost = findAndVerifyOwnership(adminEmail, jobPostId);
        return mapEntityToResponse(jobPost);
    }

    // ─── Update (full replace) ────────────────────────────────────────────────

    /**
     * Fully replaces all mutable fields of an existing job post.
     * Ownership check ensures admin A cannot edit admin B's posts.
     */
    @Transactional
    public JobPostResponse update(String adminEmail, Long jobPostId, JobPostRequest request) {
        JobPost jobPost = findAndVerifyOwnership(adminEmail, jobPostId);
        validateSalaryRange(request);

        mapRequestToEntity(jobPost, request);

        JobPost saved = jobPostRepository.save(jobPost);
        log.info("JobPost updated: id={} by admin={}", saved.getId(), adminEmail);
        return mapEntityToResponse(saved);
    }

    // ─── Patch Status ─────────────────────────────────────────────────────────

    /**
     * Changes only the {@link JobPostStatus} of a job post.
     * Used by the "Active / Paused / Closed" toggle in the UI.
     *
     * Business rule: A CLOSED post cannot be re-opened.
     */
    @Transactional
    public JobPostResponse updateStatus(String adminEmail, Long jobPostId, JobPostStatus newStatus) {
        JobPost jobPost = findAndVerifyOwnership(adminEmail, jobPostId);

        if (jobPost.getStatus() == JobPostStatus.CLOSED
                && newStatus != JobPostStatus.CLOSED) {
            throw new IllegalStateException(
                    "A closed job post cannot be reopened. Create a new posting instead.");
        }

        jobPost.setStatus(newStatus);
        JobPost saved = jobPostRepository.save(jobPost);
        log.info("JobPost status changed: id={} status={} by admin={}", saved.getId(), newStatus, adminEmail);
        return mapEntityToResponse(saved);
    }

    // ─── Delete ───────────────────────────────────────────────────────────────

    /**
     * Permanently deletes a job post.
     * Ownership check is always applied before deletion.
     */
    @Transactional
    public void delete(String adminEmail, Long jobPostId) {
        JobPost jobPost = findAndVerifyOwnership(adminEmail, jobPostId);
        jobPostRepository.delete(jobPost);
        log.info("JobPost deleted: id={} by admin={}", jobPostId, adminEmail);
    }

    // ─── Internal Helpers ─────────────────────────────────────────────────────

    /**
     * Resolves the Admin entity from email, throwing 404 if not found.
     */
    private Admin resolveAdmin(String email) {
        return adminRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Admin not found: " + email));
    }

    /**
     * Loads a job post and verifies the calling admin owns it.
     * Throws 404 if the post does not exist, 403 if it belongs to another admin.
     */
    private JobPost findAndVerifyOwnership(String adminEmail, Long jobPostId) {
        JobPost jobPost = jobPostRepository.findById(jobPostId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Job post not found with id: " + jobPostId));

        if (!jobPost.getAdmin().getEmail().equalsIgnoreCase(adminEmail)) {
            log.warn("Ownership violation: admin={} attempted to access jobPostId={}", adminEmail, jobPostId);
            throw new UnauthorizedException("You do not have permission to modify this job post.");
        }

        return jobPost;
    }

    /**
     * Validates that salaryMin <= salaryMax when both are provided.
     */
    private void validateSalaryRange(JobPostRequest request) {
        if (request.getSalaryMin() != null && request.getSalaryMax() != null) {
            if (request.getSalaryMin() > request.getSalaryMax()) {
                throw new IllegalArgumentException(
                        "Minimum salary cannot be greater than maximum salary.");
            }
        }
    }

    // ─── Mapper: Request → Entity ─────────────────────────────────────────────

    private JobPost mapRequestToEntity(JobPost jobPost, JobPostRequest request) {
        jobPost.setTitle(request.getTitle().trim());
        jobPost.setCategory(request.getCategory());
        jobPost.setJobType(request.getJobType() != null ? request.getJobType() : "Full-time");
        jobPost.setWorkType(request.getWorkType() != null ? request.getWorkType() : "Remote");
        jobPost.setLocation(request.getLocation().trim());
        jobPost.setExperienceRequired(request.getExperienceRequired().trim());
        jobPost.setNumberOfOpenings(request.getNumberOfOpenings());
        jobPost.setSalaryMin(request.getSalaryMin());
        jobPost.setSalaryMax(request.getSalaryMax());
        jobPost.setApplicationDeadline(request.getApplicationDeadline());
        jobPost.setDescription(request.getDescription().trim());
        jobPost.setResponsibilities(request.getResponsibilities());
        jobPost.setRequirementsAndQualifications(request.getRequirementsAndQualifications());

        // Replace tech stack list cleanly
        List<String> cleaned = new ArrayList<>();
        if (request.getTechStack() != null) {
            request.getTechStack().stream()
                    .filter(s -> s != null && !s.isBlank())
                    .map(String::trim)
                    .forEach(cleaned::add);
        }
        jobPost.setTechStack(cleaned);

        return jobPost;
    }

    // ─── Mapper: Entity → Response ───────────────────────────────────────────

    public JobPostResponse mapEntityToResponse(JobPost jobPost) {
        JobPostResponse response = new JobPostResponse();
        response.setId(jobPost.getId());
        response.setTitle(jobPost.getTitle());
        response.setCategory(jobPost.getCategory());
        response.setJobType(jobPost.getJobType());
        response.setWorkType(jobPost.getWorkType());
        response.setLocation(jobPost.getLocation());
        response.setExperienceRequired(jobPost.getExperienceRequired());
        response.setNumberOfOpenings(jobPost.getNumberOfOpenings());
        response.setSalaryMin(jobPost.getSalaryMin());
        response.setSalaryMax(jobPost.getSalaryMax());
        response.setApplicationDeadline(jobPost.getApplicationDeadline());
        response.setTechStack(jobPost.getTechStack());
        response.setDescription(jobPost.getDescription());
        response.setResponsibilities(jobPost.getResponsibilities());
        response.setRequirementsAndQualifications(jobPost.getRequirementsAndQualifications());
        response.setStatus(jobPost.getStatus());
        response.setCreatedAt(jobPost.getCreatedAt());
        response.setUpdatedAt(jobPost.getUpdatedAt());
        response.setPostedAgo(computePostedAgo(jobPost.getCreatedAt()));
        return response;
    }

    // ─── Utility: "posted X ago" ──────────────────────────────────────────────

    private String computePostedAgo(LocalDateTime createdAt) {
        if (createdAt == null) return "Unknown";

        LocalDateTime now = LocalDateTime.now();
        long minutes = ChronoUnit.MINUTES.between(createdAt, now);

        if (minutes < 1)    return "Just now";
        if (minutes < 60)   return minutes + " minute" + (minutes == 1 ? "" : "s") + " ago";

        long hours = ChronoUnit.HOURS.between(createdAt, now);
        if (hours < 24)     return hours + " hour" + (hours == 1 ? "" : "s") + " ago";

        long days = ChronoUnit.DAYS.between(createdAt, now);
        if (days < 7)       return days + " day" + (days == 1 ? "" : "s") + " ago";

        long weeks = days / 7;
        if (weeks < 5)      return weeks + " week" + (weeks == 1 ? "" : "s") + " ago";

        long months = ChronoUnit.MONTHS.between(createdAt, now);
        if (months < 12)    return months + " month" + (months == 1 ? "" : "s") + " ago";

        long years = ChronoUnit.YEARS.between(createdAt, now);
        return years + " year" + (years == 1 ? "" : "s") + " ago";
    }

    // ─── Specification builders ───────────────────────────────────────────────

    private Specification<JobPost> adminEquals(Long adminId) {
        return (root, query, cb) -> cb.equal(root.get("admin").get("id"), adminId);
    }

    private Specification<JobPost> titleContains(String keyword) {
        return (root, query, cb) ->
                cb.like(cb.lower(root.get("title")), "%" + keyword.toLowerCase() + "%");
    }

    private Specification<JobPost> categoryEquals(String category) {
        return (root, query, cb) -> cb.equal(root.get("category"), category);
    }

    private Specification<JobPost> statusEquals(JobPostStatus status) {
        return (root, query, cb) -> cb.equal(root.get("status"), status);
    }

    private Specification<JobPost> jobTypeEquals(String jobType) {
        return (root, query, cb) -> cb.equal(root.get("jobType"), jobType);
    }

    private Specification<JobPost> workTypeEquals(String workType) {
        return (root, query, cb) -> cb.equal(root.get("workType"), workType);
    }
}