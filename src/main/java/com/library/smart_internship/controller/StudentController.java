package com.library.smart_internship.controller;

import com.library.smart_internship.entity.Application;
import com.library.smart_internship.entity.Internship;
import com.library.smart_internship.entity.Student;
import com.library.smart_internship.repository.ApplicationRepository;
import com.library.smart_internship.repository.InternshipRepository;
import com.library.smart_internship.repository.StudentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.Principal;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/student")
@RequiredArgsConstructor
public class StudentController {

    private final InternshipRepository internshipRepository;
    private final StudentRepository studentRepository;
    private final ApplicationRepository applicationRepository;

    // Directory where resumes get stored. On Render's free tier this disk is
    // ephemeral (wiped on redeploy) - fine for now, but move to S3/Cloudinary
    // before this matters in production.
    private static final String UPLOAD_DIR = "uploads/resumes";

    @GetMapping("/dashboard")
    public String studentDashboard(@RequestParam(value = "keyword", required = false) String keyword,
                                   Principal principal,
                                   Model model) {
        String email = principal.getName();
        Student student = studentRepository.findAll().stream()
                .filter(s -> s.getEmail().equals(email))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Student account not found"));

        List<Internship> availableInternships;
        if (keyword != null && !keyword.trim().isEmpty()) {
            availableInternships = internshipRepository
                    .findByTitleContainingIgnoreCaseOrSkillsRequiredContainingIgnoreCase(keyword, keyword);
        } else {
            availableInternships = internshipRepository.findAll();
        }

        // Only show programs that are active. Full programs are still shown
        // (with 0 slots) so students can see they exist, but the template
        // disables the Apply button for those.
        availableInternships = availableInternships.stream()
                .filter(Internship::isActive)
                .collect(Collectors.toList());

        List<Application> myApplications = applicationRepository.findByStudentId(student.getId());

        model.addAttribute("student", student);
        model.addAttribute("internships", availableInternships);
        model.addAttribute("myApplications", myApplications);
        model.addAttribute("keyword", keyword);

        return "dashboard";
    }

    @PostMapping("/submit-application")
    public String applyToInternship(@RequestParam("internshipId") Long internshipId,
                                    @RequestParam(value = "file", required = false) MultipartFile file,
                                    Principal principal) throws IOException {
        String email = principal.getName();
        Student student = studentRepository.findAll().stream()
                .filter(s -> s.getEmail().equals(email))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Student account not found"));

        Internship internship = internshipRepository.findById(internshipId)
                .orElseThrow(() -> new RuntimeException("Internship not found"));

        Application application = new Application();
        application.setStudent(student);
        application.setInternship(internship);
        application.setStatus("PENDING");

        if (file != null && !file.isEmpty()) {
            application.setResumePath(saveResumeFile(file));
        }

        applicationRepository.save(application);

        return "redirect:/student/dashboard";
    }

    private String saveResumeFile(MultipartFile file) throws IOException {
        Path uploadPath = Paths.get(UPLOAD_DIR);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        String originalName = file.getOriginalFilename() != null ? file.getOriginalFilename() : "resume";
        String extension = "";
        int dotIndex = originalName.lastIndexOf('.');
        if (dotIndex >= 0) {
            extension = originalName.substring(dotIndex);
        }

        String storedFilename = UUID.randomUUID() + extension;
        Path destination = uploadPath.resolve(storedFilename);

        Files.copy(file.getInputStream(), destination, StandardCopyOption.REPLACE_EXISTING);

        return destination.toAbsolutePath().toString();
    }
}