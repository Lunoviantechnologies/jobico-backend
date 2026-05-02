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
    public ResponseEntity<Page<CandidateResponse>> getCandidates(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String skill,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) Integer minExp,
            @RequestParam(required = false) Integer maxExp,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        return ResponseEntity.ok(
                candidateService.getCandidates(name, skill, category, role, minExp, maxExp, page, size)
        );
    }

    @GetMapping("/candidates/{id}")
    public ResponseEntity<CandidateResponse> getCandidateById(@PathVariable Long id) {
        return ResponseEntity.ok(candidateService.getById(id));
    }
    @PatchMapping("/candidates/{id}/status")
    public ResponseEntity<CandidateResponse> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody StatusUpdateRequest request) {
        return ResponseEntity.ok(candidateService.updateStatus(id, request.getStatus()));
    }
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
        		.contentType(MediaType.APPLICATION_PDF)
        		.header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"offer_letter.pdf\"")
                .body(content);
    }

    // ─── Experience Letter 

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
 // Send Offer Letter via Email 

    /**
     * POST /api/admin/offer-letter/send
     * Generates Offer Letter PDF and emails it directly to candidate.
     * Body: { "candidateId": 1, "salary": 600000.00, "joiningDate": "2026-05-01" }
     * Candidate must be SELECTED and must have an email on file.
     */
    @PostMapping("/offer-letter/send")
    public ResponseEntity<ApiResponse> sendOfferLetterByEmail(
            @Valid @RequestBody OfferLetterRequest request) {
        documentService.sendOfferLetterByEmail(request);
        return ResponseEntity.ok(new ApiResponse(true, "Offer letter sent to candidate's email successfully.", 200));
    }

    // ─── Send Experience Letter via Email ────────────────────────────────────────

    /**
     * POST /api/admin/experience-letter/send
     * Generates Experience Letter PDF and emails it directly to employee.
     * Body: { "employeeId": 1, "remarks": "Outstanding contributor." }
     * Employee must have an email on file.
     */
    @PostMapping("/experience-letter/send")
    public ResponseEntity<ApiResponse> sendExperienceLetterByEmail(
            @Valid @RequestBody ExperienceLetterRequest request) {
        documentService.sendExperienceLetterByEmail(request);
        return ResponseEntity.ok(new ApiResponse(true, "Experience letter sent to employee's email successfully.",200));
    }
}