package com.library.smart_internship.controller;

import com.library.smart_internship.dto.ApplicationResponse;
import com.library.smart_internship.service.ApplicationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
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

    @GetMapping("/mine")
    public ResponseEntity<List<ApplicationResponse>> getMyApplications(
            @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(applicationService.getApplicationsForStudent(jwt.getSubject()));
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
}
