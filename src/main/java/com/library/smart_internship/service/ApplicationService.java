package com.library.smart_internship.service;

import com.library.smart_internship.entity.Application;
import com.library.smart_internship.entity.Internship;
import com.library.smart_internship.entity.Student;
import com.library.smart_internship.repository.ApplicationRepository;
import com.library.smart_internship.repository.InternshipRepository;
import com.library.smart_internship.repository.StudentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ApplicationService {

    private final ApplicationRepository applicationRepository;
    private final StudentRepository studentRepository;
    private final InternshipRepository internshipRepository;

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
}