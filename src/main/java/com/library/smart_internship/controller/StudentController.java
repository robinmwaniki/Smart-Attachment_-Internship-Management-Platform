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
import org.springframework.web.multipart.MultipartFile; // <-- Make sure to import this

import java.security.Principal;
import java.util.List;

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

        List<Application> myApplications = applicationRepository.findByStudentId(student.getId());

        model.addAttribute("student", student);
        model.addAttribute("internships", availableInternships);
        model.addAttribute("myApplications", myApplications);
        model.addAttribute("keyword", keyword);

        return "dashboard";
    }

    @PostMapping("/submit-application")
    public String applyToInternship(@RequestParam("internshipId") Long internshipId,
                                    @RequestParam(value = "file", required = false) MultipartFile file, // <-- Added this line to catch the CV
                                    Principal principal) {
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

        // NOTE: The file is being received from the frontend here, but it isn't being saved anywhere yet.
        // If your database has a column for resumes, you would process the file here!

        applicationRepository.save(application);

        return "redirect:/student/dashboard";
    }
}