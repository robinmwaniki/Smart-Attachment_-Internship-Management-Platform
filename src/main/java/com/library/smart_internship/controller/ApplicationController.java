package com.library.smart_internship.controller;

import com.library.smart_internship.dto.ApplicationResponse;
import com.library.smart_internship.dto.ResumeFile;
import com.library.smart_internship.service.ApplicationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/api/applications")
@RequiredArgsConstructor
public class ApplicationController {

    private final ApplicationService applicationService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApplicationResponse> applyForInternship(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam Long internshipId,
            @RequestParam(value = "file", required = false) MultipartFile file) throws IOException {
        ApplicationResponse response = applicationService.submitForAuthenticatedStudent(
                jwt.getSubject(), internshipId, file);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/internship/{internshipId}")
    public ResponseEntity<List<ApplicationResponse>> getApplicationsByInternship(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long internshipId) {
        return ResponseEntity.ok(
                applicationService.getApplicationsForRecruiter(jwt.getSubject(), internshipId));
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<ApplicationResponse> updateStatus(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long id,
            @RequestParam String status) {
        return ResponseEntity.ok(
                applicationService.updateStatusForRecruiter(jwt.getSubject(), id, status));
    }

    /**
     * Resume bytes are returned only after the service confirms that the
     * caller owns the application or owns the company internship.
     * Always serve uploaded files as attachments to prevent browser execution.
     */
    @GetMapping("/download/{id}")
    public ResponseEntity<byte[]> downloadResume(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long id) {
        ResumeFile resume = applicationService.downloadResumeForPrincipal(jwt.getSubject(), id);
        String safeFilename = resume.filename() == null || resume.filename().isBlank()
                ? "resume" : resume.filename().replaceAll("[\\\\/\\r\\n]", "_");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        headers.setContentDisposition(ContentDisposition.attachment()
                .filename(safeFilename, StandardCharsets.UTF_8)
                .build());
        headers.setContentLength(resume.data().length);
        headers.setXContentTypeOptions("nosniff");

        return ResponseEntity.ok().headers(headers).body(resume.data());
    }
}
