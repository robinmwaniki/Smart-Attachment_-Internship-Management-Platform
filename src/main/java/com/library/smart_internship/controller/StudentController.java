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
import java.security.Principal;
import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/student")
@RequiredArgsConstructor
public class StudentController {

    private final InternshipRepository internshipRepository;
    private final StudentRepository studentRepository;
    private final ApplicationRepository applicationRepository;

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
            application.setResumeData(file.getBytes());
            application.setResumeFilename(file.getOriginalFilename());
            application.setResumeContentType(file.getContentType());
        }

        applicationRepository.save(application);

        return "redirect:/student/dashboard";
    }
}