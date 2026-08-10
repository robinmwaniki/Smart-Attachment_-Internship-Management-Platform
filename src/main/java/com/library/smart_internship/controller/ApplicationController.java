package com.library.smart_internship.controller;

import com.library.smart_internship.entity.Application;
import com.library.smart_internship.service.ApplicationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/applications")
@RequiredArgsConstructor
public class ApplicationController {

    private final ApplicationService applicationService;

    /**
     * POST: Apply for an internship with a file attachment (Resume PDF/Word)
     * URL: http://localhost:8080/api/applications?studentId=1&internshipId=1
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Application> applyForInternship(
            @RequestParam Long studentId,
            @RequestParam Long internshipId,
            @RequestParam("file") MultipartFile file) {
        try {
            Application application = applicationService.applyWithResume(studentId, internshipId, file);
            return ResponseEntity.status(HttpStatus.CREATED).body(application);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * GET: View all applications submitted for a specific internship
     */
    @GetMapping("/internship/{internshipId}")
    public ResponseEntity<List<Application>> getApplicationsByInternship(@PathVariable Long internshipId) {
        List<Application> applications = applicationService.getApplicationsForInternship(internshipId);
        return ResponseEntity.ok(applications);
    }

    /**
     * PUT: Recruiter updates application status (APPROVED / REJECTED)
     * URL: http://localhost:8080/api/applications/1/status?status=APPROVED
     */
    @PutMapping("/{id}/status")
    public ResponseEntity<Application> updateStatus(
            @PathVariable Long id,
            @RequestParam String status) {
        Application updatedApplication = applicationService.updateApplicationStatus(id, status);
        return ResponseEntity.ok(updatedApplication);
    }
}