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

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ApplicationService {

    private final ApplicationRepository applicationRepository;
    private final StudentRepository studentRepository;
    private final InternshipRepository internshipRepository;

    // Folder path where attachments will be saved
    private final String UPLOAD_DIR = "uploads/resumes/";

    public Application applyWithResume(Long studentId, Long internshipId, MultipartFile file) throws IOException {
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new RuntimeException("Student not found with id: " + studentId));

        Internship internship = internshipRepository.findById(internshipId)
                .orElseThrow(() -> new RuntimeException("Internship not found with id: " + internshipId));

        // Ensure the upload directory exists
        File directory = new File(UPLOAD_DIR);
        if (!directory.exists()) {
            directory.mkdirs();
        }

        // Generate a unique file name to prevent overwriting files with the same name
        String fileName = System.currentTimeMillis() + "_" + file.getOriginalFilename();
        Path filePath = Paths.get(UPLOAD_DIR + fileName);

        // Save file to disk
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

        // Save application record to database
        Application application = new Application();
        application.setStudent(student);
        application.setInternship(internship);
        application.setStatus("PENDING");
        application.setResumePath(filePath.toString());

        return applicationRepository.save(application);
    }

    public List<Application> getApplicationsForInternship(Long internshipId) {
        return applicationRepository.findByInternshipId(internshipId);
    }

    // NEW: Recruiter status update method
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