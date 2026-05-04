package com.example.jobico.controller;

import com.example.jobico.dto.*;
import com.example.jobico.entity.EmployeeStatus;
import com.example.jobico.service.*;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    @Autowired private CandidateService           candidateService;
    @Autowired private FileService                fileService;
    @Autowired private DocumentService            documentService;
    @Autowired private EmployeeManagementService  employeeManagementService;

    // ── Candidates 

    @GetMapping("/candidates/all")
    public ResponseEntity<Page<CandidateResponse>> getCandidates(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String category,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(candidateService.getCandidates(search, status, category, page, size));
    }

    @GetMapping("/candidates/{id}")
    public ResponseEntity<CandidateResponse> getCandidateById(@PathVariable Long id) {
        return ResponseEntity.ok(candidateService.getById(id));
    }

    @PatchMapping("/candidates/{id}/status")
    public ResponseEntity<CandidateResponse> updateStatus(
            @PathVariable Long id, @Valid @RequestBody StatusUpdateRequest request) {
        return ResponseEntity.ok(candidateService.updateStatus(id, request.getStatus()));
    }

    @GetMapping("/candidates/{id}/resume")
    public ResponseEntity<Resource> downloadResume(@PathVariable Long id) {
        String resumePath = candidateService.getResumePath(id);
        Resource resource = fileService.loadResumeAsResource(resumePath);
        String contentType = "application/octet-stream";
        String filename    = resource.getFilename();
        if (filename != null) {
            if (filename.endsWith(".pdf"))  contentType = "application/pdf";
            else if (filename.endsWith(".doc"))  contentType = "application/msword";
            else if (filename.endsWith(".docx"))
                contentType = "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
        }
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .body(resource);
    }

    @GetMapping("/candidates/selected")
    public ResponseEntity<Page<CandidateResponse>> getSelectedCandidates(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String category,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        // Only returns SELECTED candidates — i.e. those whose offer letter has NOT yet been generated
        return ResponseEntity.ok(candidateService.getCandidates(search, "SELECTED", category, page, size));
    }

   
    @GetMapping("/candidates/offer-letter-generated")
    public ResponseEntity<Page<CandidateResponse>> getOfferLetterGeneratedCandidates(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String category,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(candidateService.getCandidates(search, "OFFER_LETTER_GENERATED", category, page, size));
    }

    // ── Offer Letters 

   
    @PostMapping("/offer-letter/generate")
    public ResponseEntity<OfferLetterResponse> generateOfferLetter(
            @Valid @RequestBody OfferLetterRequest request) {
        return ResponseEntity.ok(documentService.generateAndSaveOfferLetter(request));
    }

    @GetMapping("/offer-letter/{id}/download")
    public ResponseEntity<byte[]> downloadOfferLetter(@PathVariable Long id) {
        byte[] pdf = documentService.downloadOfferLetter(id);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"offer_letter.pdf\"")
                .body(pdf);
    }

    /**
     * STEP 2b — Email the already-generated PDF to the candidate.
     */
    @PostMapping("/offer-letter/{id}/send")
    public ResponseEntity<ApiResponse> sendOfferLetterByEmail(@PathVariable Long id) {
        documentService.sendOfferLetterByEmail(id);
        return ResponseEntity.ok(new ApiResponse(true, "Offer letter sent to candidate's email successfully.", 200));
    }

    /**
     * One-shot — generate + persist + email atomically.
     */
    @PostMapping("/offer-letter/generate-and-send")
    public ResponseEntity<OfferLetterResponse> generateAndSendOfferLetter(
            @Valid @RequestBody OfferLetterRequest request) {
        return ResponseEntity.ok(documentService.generateSaveAndSendOfferLetter(request));
    }

   
    @GetMapping("/offer-letter")
    public ResponseEntity<Page<OfferLetterResponse>> listOfferLetters(
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(documentService.listOfferLetters(search, page, size));
    }

    /**
     * History — all offer letters for a specific candidate.
     */
    @GetMapping("/candidates/{id}/offer-letters")
    public ResponseEntity<List<OfferLetterResponse>> getOfferLettersForCandidate(@PathVariable Long id) {
        return ResponseEntity.ok(documentService.getOfferLettersForCandidate(id));
    }

    // ── Experience Letters ────────────────────────────────────────────────────

    /**
     * STEP 1 — Generate and persist the experience letter PDF.
     *
     * POST /api/admin/experience-letter/generate
     * { "employeeId": 1, "remarks": "Outstanding contributor." }
     */
    @PostMapping("/experience-letter/generate")
    public ResponseEntity<ExperienceLetterResponse> generateExperienceLetter(
            @Valid @RequestBody ExperienceLetterRequest request) {
        return ResponseEntity.ok(documentService.generateAndSaveExperienceLetter(request));
    }

    /**
     * STEP 2a — Preview / download the stored PDF.
     *
     * GET /api/admin/experience-letter/{id}/download
     */
    @GetMapping("/experience-letter/{id}/download")
    public ResponseEntity<byte[]> downloadExperienceLetter(@PathVariable Long id) {
        byte[] pdf = documentService.downloadExperienceLetter(id);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"experience_letter.pdf\"")
                .body(pdf);
    }

    /**
     * STEP 2b — Email the already-generated PDF to the employee.
     *
     * POST /api/admin/experience-letter/{id}/send
     */
    @PostMapping("/experience-letter/{id}/send")
    public ResponseEntity<ApiResponse> sendExperienceLetterByEmail(@PathVariable Long id) {
        documentService.sendExperienceLetterByEmail(id);
        return ResponseEntity.ok(new ApiResponse(true, "Experience letter sent to employee's email successfully.", 200));
    }

    /**
     * One-shot — generate + persist + email.
     *
     * POST /api/admin/experience-letter/generate-and-send
     */
    @PostMapping("/experience-letter/generate-and-send")
    public ResponseEntity<ExperienceLetterResponse> generateAndSendExperienceLetter(
            @Valid @RequestBody ExperienceLetterRequest request) {
        return ResponseEntity.ok(documentService.generateSaveAndSendExperienceLetter(request));
    }

    /**
     * Admin dashboard — paginated list of all experience letters.
     *
     * GET /api/admin/experience-letter?search=&page=0&size=10
     */
    @GetMapping("/experience-letter")
    public ResponseEntity<Page<ExperienceLetterResponse>> listExperienceLetters(
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(documentService.listExperienceLetters(search, page, size));
    }

    /**
     * History — all experience letters for a specific employee.
     *
     * GET /api/admin/employees/{id}/experience-letters
     */
    @GetMapping("/employees/{id}/experience-letters")
    public ResponseEntity<List<ExperienceLetterResponse>> getExperienceLettersForEmployee(@PathVariable Long id) {
        return ResponseEntity.ok(documentService.getExperienceLettersForEmployee(id));
    }

    // ── Employees

    @GetMapping("/employees/exited")
    public ResponseEntity<Page<EmployeeListResponse>> getExitedEmployees(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String department,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Page<EmployeeListResponse> result =
                employeeManagementService.listExitedEmployees(search, department, page, size);

        return ResponseEntity.ok(result);
    }
}