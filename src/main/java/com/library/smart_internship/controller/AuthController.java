package com.library.smart_internship.controller;

import com.library.smart_internship.dto.MatchResult;
import com.library.smart_internship.entity.Application;
import com.library.smart_internship.entity.Student;
import com.library.smart_internship.repository.ApplicationRepository;
import com.library.smart_internship.repository.StudentRepository;
import com.library.smart_internship.service.ApplicationService;
import com.library.smart_internship.service.MatchingService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.util.List;

@Controller
@RequiredArgsConstructor
public class AuthController {

    private final StudentRepository studentRepository;
    private final ApplicationRepository applicationRepository;
    private final ApplicationService applicationService;
    private final MatchingService matchingService;
    private final PasswordEncoder passwordEncoder;

    @GetMapping("/login")
    public String loginPage() { return "login"; }

    @GetMapping("/register")
    public String registerPage() { return "register"; }

    @PostMapping("/register")
    public String registerUser(@ModelAttribute Student student) {

        // 1. Security Check: Verify if email already exists in the database
        boolean emailExists = studentRepository.findAll().stream()
                .anyMatch(s -> s.getEmail().equalsIgnoreCase(student.getEmail()));

        if (emailExists) {
            // Redirect back to the registration page with an error parameter
            return "redirect:/register?error";
        }

        // 2. Hash the password securely
        student.setPassword(passwordEncoder.encode(student.getPassword()));

        // 3. Ensure a fallback just in case the role was somehow left empty
        if (student.getRole() == null || student.getRole().isEmpty()) {
            student.setRole("STUDENT");
        }

        // 4. Save the new user
        studentRepository.save(student);
        return "redirect:/login?success";
    }

    @GetMapping("/dashboard")
    public String dashboardPage(Principal principal, Model model) {
        String email = principal.getName();
        Student student = studentRepository.findAll().stream()
                .filter(s -> s.getEmail().equals(email))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Student not found"));

        // 1. Get smart internship matches
        List<MatchResult> recommendations = matchingService.getRecommendationsForStudent(student.getId());

        // 2. Get actual applications submitted by this specific student
        List<Application> myApplications = applicationRepository.findByStudentId(student.getId());

        model.addAttribute("student", student);
        model.addAttribute("recommendations", recommendations);
        model.addAttribute("myApplications", myApplications);

        return "dashboard";
    }

    @PostMapping("/student/apply")
    public String handleWebApplication(
            @RequestParam Long studentId,
            @RequestParam Long internshipId,
            @RequestParam("file") MultipartFile file,
            RedirectAttributes redirectAttributes) {
        try {
            applicationService.applyWithResume(studentId, internshipId, file);
            redirectAttributes.addFlashAttribute("message", "Application submitted successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to submit application: " + e.getMessage());
        }
        return "redirect:/dashboard";
    }
}