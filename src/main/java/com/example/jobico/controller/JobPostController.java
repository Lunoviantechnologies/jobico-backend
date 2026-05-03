package com.example.jobico.controller;

import com.example.jobico.dto.ApiResponse;
import com.example.jobico.dto.JobPostRequest;
import com.example.jobico.dto.JobPostResponse;
import com.example.jobico.dto.StatusUpdateRequest;
import com.example.jobico.entity.JobPostStatus;
import com.example.jobico.service.JobPostService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api/admin/jobs")
public class JobPostController {

    @Autowired private JobPostService jobPostService;


    @PostMapping
    public ResponseEntity<ApiResponse<JobPostResponse>> create(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody JobPostRequest request) {

        JobPostResponse created = jobPostService.create(userDetails.getUsername(), request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(new ApiResponse<>(true, "Job post created successfully.", created, 201));
    }

    // ─── List / Search

    /**
     * GET /api/admin/jobs
     *
     * Returns a paginated list of the authenticated admin's job posts.
     * All query parameters are optional and are combined with AND logic.
     *
     **/
    @GetMapping
    public ResponseEntity<Page<JobPostResponse>> getAll(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) JobPostStatus status,
            @RequestParam(required = false) String jobType,
            @RequestParam(required = false) String workType,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "10") int size) {

        return ResponseEntity.ok(
                jobPostService.getAll(
                        userDetails.getUsername(),
                        keyword, category, status, jobType, workType,
                        page, size));
    }

    // ─── Get Single 
    @GetMapping("/{id}")
    public ResponseEntity<JobPostResponse> getById(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id) {

        return ResponseEntity.ok(
                jobPostService.getById(userDetails.getUsername(), id));
    }

    // ─── Full Update 

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<JobPostResponse>> update(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id,
            @Valid @RequestBody JobPostRequest request) {

        JobPostResponse updated = jobPostService.update(userDetails.getUsername(), id, request);

        return ResponseEntity.ok(
                new ApiResponse<>(true, "Job post updated successfully.", updated, 200));
    }

    // ─── Patch Status 

    /**
     * PATCH /api/admin/jobs/{id}/status
     *
     * Changes only the status of a job post.
     * Reuses the existing {@link StatusUpdateRequest} DTO from the project
     * — the frontend sends: { "status": "PAUSED" }
     *
     * Note: Status value must match the {@link JobPostStatus} enum exactly
     * (DRAFT | ACTIVE | PAUSED | CLOSED).
     *
     * Business rule: CLOSED posts cannot be re-opened.
     *
     * Response: 200 OK — JobPostResponse
     */
    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<JobPostResponse>> updateStatus(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id,
            @RequestBody JobPostStatusUpdateRequest request) {

        JobPostResponse updated = jobPostService.updateStatus(
                userDetails.getUsername(), id, request.getStatus());

        return ResponseEntity.ok(
                new ApiResponse<>(true, "Job post status updated to " + request.getStatus() + ".", updated, 200));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id) {

        jobPostService.delete(userDetails.getUsername(), id);

        return ResponseEntity.ok(
                new ApiResponse<>(true, "Job post deleted successfully.", 200));
    }

    // ─── Inner DTO 
    public static class JobPostStatusUpdateRequest {

        private JobPostStatus status;

        public JobPostStatus getStatus() { return status; }
        public void setStatus(JobPostStatus status) { this.status = status; }
    }
}