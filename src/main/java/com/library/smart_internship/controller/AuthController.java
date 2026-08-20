package com.library.smart_internship.controller;


import com.library.smart_internship.entity.PasswordResetToken;
import com.library.smart_internship.entity.Student;
import com.library.smart_internship.repository.ApplicationRepository;
import com.library.smart_internship.repository.PasswordResetTokenRepository;
import com.library.smart_internship.repository.StudentRepository;
import com.library.smart_internship.service.ApplicationService;
import com.library.smart_internship.service.EmailService;
import com.library.smart_internship.service.MatchingService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;


import java.time.LocalDateTime;

import java.util.UUID;

@Controller
@RequiredArgsConstructor
public class AuthController {

    private final StudentRepository studentRepository;
    private final ApplicationRepository applicationRepository;
    private final ApplicationService applicationService;
    private final MatchingService matchingService;
    private final PasswordEncoder passwordEncoder;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final EmailService emailService;

    @GetMapping("/login")
    public String loginPage() { return "login"; }

    @GetMapping("/register")
    public String registerPage() { return "register"; }

    @PostMapping("/register")
    public String registerUser(@ModelAttribute Student student) {

        boolean emailExists = studentRepository.findAll().stream()
                .anyMatch(s -> s.getEmail().equalsIgnoreCase(student.getEmail()));

        if (emailExists) {
            return "redirect:/register?error";
        }

        student.setPassword(passwordEncoder.encode(student.getPassword()));

        if (student.getRole() == null || student.getRole().isEmpty()) {
            student.setRole("STUDENT");
        }

        studentRepository.save(student);
        return "redirect:/login?success";
    }

    @GetMapping("/dashboard")
    public String legacyDashboardRedirect() {
        return "redirect:/student/dashboard";
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
        return "redirect:/student/dashboard";
    }

    @GetMapping("/forgot-password")
    public String forgotPasswordPage() {
        return "forgot-password";
    }

    @PostMapping("/forgot-password")
    public String processForgotPassword(@RequestParam("email") String email,
                                        HttpServletRequest request,
                                        Model model) {
        var studentOpt = studentRepository.findAll().stream()
                .filter(s -> s.getEmail().equals(email))
                .findFirst();

        if (studentOpt.isPresent()) {
            var student = studentOpt.get();

            PasswordResetToken resetToken = new PasswordResetToken();
            resetToken.setToken(UUID.randomUUID().toString());
            resetToken.setStudent(student);
            resetToken.setExpiryDate(LocalDateTime.now().plusMinutes(30));
            passwordResetTokenRepository.save(resetToken);

            String baseUrl = request.getRequestURL().toString().replace(request.getRequestURI(), "");
            String resetLink = baseUrl + "/reset-password?token=" + resetToken.getToken();

            try {
                emailService.sendPasswordResetEmail(student.getEmail(), resetLink);
            } catch (Exception e) {
                System.err.println("Password reset email failed: " + e.getMessage());
            }
        }

        model.addAttribute("message", "If an account exists with that email, a reset link has been sent.");
        return "forgot-password";
    }

    @GetMapping("/reset-password")
    public String resetPasswordPage(@RequestParam("token") String token, Model model) {
        var tokenOpt = passwordResetTokenRepository.findByToken(token);

        if (tokenOpt.isEmpty() || tokenOpt.get().isExpired()) {
            model.addAttribute("error", "This reset link is invalid or has expired.");
            return "reset-password";
        }

        model.addAttribute("token", token);
        return "reset-password";
    }

    @PostMapping("/reset-password")
    public String processResetPassword(@RequestParam("token") String token,
                                       @RequestParam("password") String password,
                                       Model model) {
        var tokenOpt = passwordResetTokenRepository.findByToken(token);

        if (tokenOpt.isEmpty() || tokenOpt.get().isExpired()) {
            model.addAttribute("error", "This reset link is invalid or has expired.");
            return "reset-password";
        }

        PasswordResetToken resetToken = tokenOpt.get();
        var student = resetToken.getStudent();
        student.setPassword(passwordEncoder.encode(password));
        studentRepository.save(student);
        passwordResetTokenRepository.delete(resetToken);

        model.addAttribute("success", "Password reset successfully. You can now log in.");
        return "reset-password";
    }
}