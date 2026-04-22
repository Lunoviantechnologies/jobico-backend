package com.example.jobico.controller;

import com.example.jobico.service.CandidateService;
import com.example.jobico.service.FileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/api")
public class FileUploadController {

    @Autowired private FileService fileService;
    @Autowired private CandidateService candidateService;

    /**
     * POST /api/resume/upload
     * Upload resume (PDF/DOC/DOCX, max 10MB).
     */
    @PostMapping("/resume/upload")
    public ResponseEntity<String> uploadResume(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam("file") MultipartFile file) throws IOException {

        if (file.isEmpty())
            return ResponseEntity.badRequest().body("Please select a file to upload");

        String savedPath = fileService.saveResume(file);
        candidateService.updateResumePath(userDetails.getUsername(), savedPath);
        return ResponseEntity.ok("Resume uploaded successfully: " + savedPath);
    }

    /**
     * GET /api/resume/download/{candidateId}
     * Download resume by candidate ID.
     */
    @GetMapping("/resume/download/{candidateId}")
    public ResponseEntity<Resource> downloadResume(@PathVariable Long candidateId) {
        String resumePath = candidateService.getResumePath(candidateId);
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
}