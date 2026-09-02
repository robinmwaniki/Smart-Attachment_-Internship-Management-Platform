package com.library.smart_internship.controller;

import com.library.smart_internship.entity.PasswordResetToken;
import com.library.smart_internship.entity.Student;
import com.library.smart_internship.repository.PasswordResetTokenRepository;
import com.library.smart_internship.repository.StudentRepository;
import com.library.smart_internship.service.EmailService;
import com.library.smart_internship.service.StudentService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Controller
@RequiredArgsConstructor
public class HomeController {

    private static final int RESET_TOKEN_VALIDITY_MINUTES = 30;

    private final StudentRepository studentRepository;
    private final StudentService studentService;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;

    @GetMapping("/")
    public String index() {
        return "index";
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/register")
    public String register() {
        return "register";
    }

    @PostMapping("/register")
    public String registerSubmit(@RequestParam String name,
                                 @RequestParam String email,
                                 @RequestParam String password,
                                 @RequestParam String role,
                                 @RequestParam(value = "skills", required = false) String skills) {
        boolean emailTaken = studentRepository.findAll().stream()
                .anyMatch(s -> s.getEmail().equalsIgnoreCase(email));

        if (emailTaken) {
            return "redirect:/register?error";
        }

        Student student = new Student();
        student.setName(name);
        student.setEmail(email);
        student.setPassword(passwordEncoder.encode(password));
        student.setRole(role);

        if ("STUDENT".equalsIgnoreCase(role) && skills != null && !skills.isBlank()) {
            student.setSkills(skills);
        }

        studentService.createStudent(student);

        return "redirect:/login?registered";
    }

    @GetMapping("/forgot-password")
    public String forgotPassword() {
        return "forgot-password";
    }

    @PostMapping("/forgot-password")
    public String forgotPasswordSubmit(@RequestParam String email, Model model) {
        Optional<Student> student = studentRepository.findAll().stream()
                .filter(s -> s.getEmail().equalsIgnoreCase(email))
                .findFirst();

        student.ifPresent(value -> {
            PasswordResetToken resetToken = new PasswordResetToken();
            resetToken.setStudent(value);
            resetToken.setToken(UUID.randomUUID().toString());
            resetToken.setExpiryDate(LocalDateTime.now().plusMinutes(RESET_TOKEN_VALIDITY_MINUTES));
            passwordResetTokenRepository.save(resetToken);

            String resetLink = ServletUriComponentsBuilder.fromCurrentContextPath()
                    .path("/reset-password")
                    .queryParam("token", resetToken.getToken())
                    .toUriString();

            try {
                emailService.sendPasswordResetEmail(value.getEmail(), resetLink);
            } catch (Exception e) {
                System.err.println("Password reset email failed to dispatch: " + e.getMessage());
            }
        });

        model.addAttribute("message", "If an account with that email exists, a reset link has been sent.");
        return "forgot-password";
    }

    @GetMapping("/reset-password")
    public String resetPassword(@RequestParam(required = false) String token, Model model) {
        model.addAttribute("token", token);
        return "reset-password";
    }

    @PostMapping("/reset-password")
    public String resetPasswordSubmit(@RequestParam String token,
                                      @RequestParam String password,
                                      Model model) {
        Optional<PasswordResetToken> resetToken = passwordResetTokenRepository.findByToken(token);

        if (resetToken.isEmpty() || resetToken.get().isExpired()) {
            model.addAttribute("error", "This reset link is invalid or has expired. Please request a new one.");
            return "reset-password";
        }

        Student student = resetToken.get().getStudent();
        student.setPassword(passwordEncoder.encode(password));
        studentRepository.save(student);

        passwordResetTokenRepository.delete(resetToken.get());

        model.addAttribute("success", "Your password has been reset successfully. You can now log in.");
        return "reset-password";
    }
}