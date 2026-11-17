package com.library.smart_internship.dto;

import java.time.LocalDateTime;

/**
 * Safe API representation of an application. It deliberately excludes JPA
 * entities, credentials, and the binary resume payload.
 */
public record ApplicationResponse(
        Long id,
        Long internshipId,
        String internshipTitle,
        String applicantName,
        Integer applicantAge,
        String applicantPhone,
        String applicantEmail,
        String coverLetter,
        String status,
        String feedback,
        String resumeFilename,
        LocalDateTime interviewDateTime
) {}
