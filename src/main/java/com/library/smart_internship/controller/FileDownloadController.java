package com.library.smart_internship.controller;

import com.library.smart_internship.entity.Application;
import com.library.smart_internship.repository.ApplicationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/applications")
@RequiredArgsConstructor
public class FileDownloadController {

    private final ApplicationRepository applicationRepository;

    @GetMapping("/download/{id}")
    public ResponseEntity<ByteArrayResource> downloadResume(@PathVariable Long id) {

        Application application = applicationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Application not found"));

        if (!application.hasResume()) {
            return ResponseEntity.notFound().build();
        }

        ByteArrayResource resource = new ByteArrayResource(application.getResumeData());

        String contentType = application.getResumeContentType() != null
                ? application.getResumeContentType()
                : "application/octet-stream";

        String filename = application.getResumeFilename() != null
                ? application.getResumeFilename()
                : "resume";

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + filename + "\"")
                .contentLength(application.getResumeData().length)
                .body(resource);
    }
}