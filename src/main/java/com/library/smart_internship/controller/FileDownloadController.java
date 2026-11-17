package com.library.smart_internship.controller;

import com.library.smart_internship.dto.ResumeFile;
import com.library.smart_internship.service.ApplicationService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/applications")
@RequiredArgsConstructor
public class FileDownloadController {

    private final ApplicationService applicationService;

    @GetMapping("/download/{id}")
    public ResponseEntity<ByteArrayResource> downloadResume(
            @PathVariable Long id,
            @AuthenticationPrincipal Jwt jwt) {
        ResumeFile resume = applicationService.downloadResumeForPrincipal(jwt.getSubject(), id);
        String disposition = ContentDisposition.attachment()
                .filename(resume.filename(), StandardCharsets.UTF_8)
                .build()
                .toString();

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition)
                .header("X-Content-Type-Options", "nosniff")
                .contentLength(resume.data().length)
                .body(new ByteArrayResource(resume.data()));
    }
}
