package com.library.smart_internship.service;

import com.library.smart_internship.dto.ApplicationResponse;
import com.library.smart_internship.dto.ResumeFile;
import com.library.smart_internship.entity.Application;
import com.library.smart_internship.entity.Internship;
import com.library.smart_internship.entity.Student;
import com.library.smart_internship.repository.ApplicationRepository;
import com.library.smart_internship.repository.InternshipRepository;
import com.library.smart_internship.repository.StudentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class ApplicationService {

    private final ApplicationRepository applicationRepository;
    private final StudentRepository studentRepository;
    private final InternshipRepository internshipRepository;

    /*
     * Legacy service methods are retained for the existing Thymeleaf
     * controllers/tests. The REST API uses the subject-based methods below.
     */
    public Application applyWithResume(Long studentId, Long internshipId, MultipartFile file) throws IOException {
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new RuntimeException("Student not found with id: " + studentId));

        Internship internship = internshipRepository.findById(internshipId)
                .orElseThrow(() -> new RuntimeException("Internship not found with id: " + internshipId));

        Application application = new Application();
        application.setStudent(student);
        application.setInternship(internship);
        application.setStatus("PENDING");

        if (file != null && !file.isEmpty()) {
            application.setResumeData(file.getBytes());
            application.setResumeFilename(file.getOriginalFilename());
            application.setResumeContentType(file.getContentType());
        }

        return applicationRepository.save(application);
    }

    public List<Application> getApplicationsForInternship(Long internshipId) {
        return applicationRepository.findByInternshipId(internshipId);
    }

    public Application updateApplicationStatus(Long applicationId, String status) {
        Application application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new RuntimeException("Application not found with id: " + applicationId));

        application.setStatus(status.toUpperCase());
        return applicationRepository.save(application);
    }

    public Application updateApplicationStatus(Long applicationId, String status, String feedback) {
        Application application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new RuntimeException("Application not found with id: " + applicationId));

        application.setStatus(status.toUpperCase());
        application.setFeedback(feedback);
        return applicationRepository.save(application);
    }

    @Transactional
    public ApplicationResponse submitForAuthenticatedStudent(
            String keycloakSubject, Long internshipId, MultipartFile file) throws IOException {
        Student student = requireLinkedStudent(keycloakSubject);
        if (!"STUDENT".equalsIgnoreCase(student.getRole())) {
            throw notFound();
        }

        Internship internship = internshipRepository.findById(internshipId).orElseThrow(this::notFound);
        if (!internship.isActive() || internship.isExpired()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This internship is not accepting applications.");
        }

        boolean alreadyApplied = applicationRepository.findByStudentId(student.getId()).stream()
                .anyMatch(existing -> existing.getInternship().getId().equals(internshipId));
        if (alreadyApplied) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "An application already exists for this internship.");
        }

        Application application = new Application();
        application.setStudent(student);
        application.setInternship(internship);
        application.setStatus("PENDING");
        application.setApplicantName(student.getName());
        application.setApplicantPhone(student.getPhone());

        if (file != null && !file.isEmpty()) {
            application.setResumeData(file.getBytes());
            application.setResumeFilename(file.getOriginalFilename());
            application.setResumeContentType(file.getContentType());
        }

        return toResponse(applicationRepository.save(application));
    }

    @Transactional(readOnly = true)
    public List<ApplicationResponse> getApplicationsForRecruiter(String keycloakSubject, Long internshipId) {
        Student recruiter = requireRecruiter(keycloakSubject);
        Internship internship = internshipRepository.findById(internshipId).orElseThrow(this::notFound);
        if (internship.getRecruiter() == null
                || !internship.getRecruiter().getId().equals(recruiter.getId())) {
            throw notFound();
        }

        return applicationRepository.findByInternshipId(internshipId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public ApplicationResponse updateStatusForRecruiter(
            String keycloakSubject, Long applicationId, String requestedStatus) {
        Student recruiter = requireRecruiter(keycloakSubject);
        if (requestedStatus == null || requestedStatus.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Application status is required.");
        }

        Application application = applicationRepository.findById(applicationId).orElseThrow(this::notFound);
        Internship internship = application.getInternship();
        if (internship == null || internship.getRecruiter() == null
                || !internship.getRecruiter().getId().equals(recruiter.getId())) {
            throw notFound();
        }

        String status = requestedStatus.trim().toUpperCase(Locale.ROOT);
        if (!List.of("PENDING", "APPROVED", "REJECTED").contains(status)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported application status.");
        }

        application.setStatus(status);
        return toResponse(applicationRepository.save(application));
    }

    @Transactional(readOnly = true)
    public ResumeFile downloadResumeForPrincipal(String keycloakSubject, Long applicationId) {
        Student principal = requireLinkedStudent(keycloakSubject);
        Application application = applicationRepository.findById(applicationId).orElseThrow(this::notFound);

        boolean isOwner = application.getStudent() != null
                && application.getStudent().getId().equals(principal.getId());
        boolean isRecruiter = isCompanyRole(principal.getRole())
                && application.getInternship() != null
                && application.getInternship().getRecruiter() != null
                && application.getInternship().getRecruiter().getId().equals(principal.getId());

        if (!isOwner && !isRecruiter) {
            throw notFound();
        }
        if (!application.hasResume()) {
            throw notFound();
        }

        String filename = application.getResumeFilename() == null
                ? "resume" : application.getResumeFilename();
        String contentType = application.getResumeContentType() == null
                ? "application/octet-stream" : application.getResumeContentType();
        return new ResumeFile(application.getResumeData(), filename, contentType);
    }

    private Student requireLinkedStudent(String subject) {
        if (subject == null || subject.isBlank()) {
            throw notFound();
        }
        return studentRepository.findByKeycloakSubject(subject).orElseThrow(this::notFound);
    }

    private Student requireRecruiter(String subject) {
        Student recruiter = requireLinkedStudent(subject);
        if (!isCompanyRole(recruiter.getRole())) {
            throw notFound();
        }
        return recruiter;
    }

    private boolean isCompanyRole(String role) {
        return role != null && ("COMPANY".equalsIgnoreCase(role) || "RECRUITER".equalsIgnoreCase(role));
    }

    private ApplicationResponse toResponse(Application application) {
        Student applicant = application.getStudent();
        Internship internship = application.getInternship();
        return new ApplicationResponse(
                application.getId(),
                internship == null ? null : internship.getId(),
                internship == null ? null : internship.getTitle(),
                application.getApplicantName() == null && applicant != null
                        ? applicant.getName() : application.getApplicantName(),
                application.getApplicantAge(),
                application.getApplicantPhone() == null && applicant != null
                        ? applicant.getPhone() : application.getApplicantPhone(),
                applicant == null ? null : applicant.getEmail(),
                application.getCoverLetter(),
                application.getStatus(),
                application.getFeedback(),
                application.getResumeFilename(),
                application.getInterviewDateTime()
        );
    }

    private ResponseStatusException notFound() {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, "The requested application resource was not found.");
    }
}
