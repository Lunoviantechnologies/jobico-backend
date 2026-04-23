package com.example.jobico.controller;

import com.example.jobico.dto.*;
import com.example.jobico.service.CandidateService;
import com.example.jobico.service.DocumentService;
import com.example.jobico.service.FileService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    @Autowired private CandidateService candidateService;
    @Autowired private FileService fileService;
    @Autowired private DocumentService documentService;


    @GetMapping("/candidates/all")
    public ResponseEntity<Page<CandidateResponse>> getAllCandidates(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(candidateService.getAll(page, size));
    }

    @GetMapping("/candidates/{id}")
    public ResponseEntity<CandidateResponse> getCandidateById(@PathVariable Long id) {
        return ResponseEntity.ok(candidateService.getById(id));
    }

    @GetMapping("/candidates/search")
    public ResponseEntity<Page<CandidateResponse>> searchByName(
            @RequestParam String name,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(candidateService.searchByName(name, page, size));
    }

    @GetMapping("/candidates/search/skill")
    public ResponseEntity<Page<CandidateResponse>> searchBySkill(
            @RequestParam String skill,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(candidateService.searchBySkill(skill, page, size));
    }

    @GetMapping("/candidates/filter")
    public ResponseEntity<Page<CandidateResponse>> filter(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) Integer minExp,
            @RequestParam(required = false) Integer maxExp,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        if (minExp != null && maxExp != null)
            return ResponseEntity.ok(candidateService.filterByExperience(minExp, maxExp, page, size));
        if (category != null && role != null)
            return ResponseEntity.ok(candidateService.filterByCategoryAndRole(category, role, page, size));
        if (category != null)
            return ResponseEntity.ok(candidateService.filterByCategory(category, page, size));
        if (role != null)
            return ResponseEntity.ok(candidateService.filterByRole(role, page, size));

        return ResponseEntity.ok(candidateService.getAll(page, size));
    }

    // ─── Status Update ───────────────────────────────────────────────────────

    @PatchMapping("/candidates/{id}/status")
    public ResponseEntity<CandidateResponse> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody StatusUpdateRequest request) {
        return ResponseEntity.ok(candidateService.updateStatus(id, request.getStatus()));
    }

    // ─── Resume Download ─────────────────────────────────────────────────────

    @GetMapping("/candidates/{id}/resume")
    public ResponseEntity<Resource> downloadResume(@PathVariable Long id) {
        String resumePath = candidateService.getResumePath(id);
        Resource resource = fileService.loadResumeAsResource(resumePath);

        String contentType = "application/octet-stream";
        String filename = resource.getFilename();
        if (filename != null) {
            if (filename.endsWith(".pdf")) contentType = "application/pdf";
            else if (filename.endsWith(".doc")) contentType = "application/msword";
            else if (filename.endsWith(".docx"))
                contentType = "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
        }

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + resource.getFilename() + "\"")
                .body(resource);
    }

    // ─── Offer Letter ────────────────────────────────────────────────────────

    /**
     * POST /api/admin/offer-letter
     * Body: { "candidateId": 1, "salary": 600000.00, "joiningDate": "2026-05-01" }
     * Only works when candidate status = SELECTED.
     */
    @PostMapping("/offer-letter")
    public ResponseEntity<byte[]> generateOfferLetter(
            @Valid @RequestBody OfferLetterRequest request) {
        byte[] content = documentService.generateOfferLetter(request);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"offer_letter.txt\"")
                .body(content);
    }

    // ─── Experience Letter ───────────────────────────────────────────────────

    /**
     * POST /api/admin/experience-letter
     * Body: { "employeeId": 1, "remarks": "Outstanding contributor." }
     */
    @PostMapping("/experience-letter")
    public ResponseEntity<byte[]> generateExperienceLetter(
            @Valid @RequestBody ExperienceLetterRequest request) {
        byte[] content = documentService.generateExperienceLetter(request);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"experience_letter.txt\"")
                .body(content);
    }
}