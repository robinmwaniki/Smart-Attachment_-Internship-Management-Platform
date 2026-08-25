package com.library.smart_internship.controller;

import com.library.smart_internship.entity.PasswordResetToken;
import com.library.smart_internship.entity.Student;
import com.library.smart_internship.repository.PasswordResetTokenRepository;
import com.library.smart_internship.repository.StudentRepository;
import com.library.smart_internship.service.EmailService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Controller
@RequiredArgsConstructor
public class WebAuthController {

    private final StudentRepository studentRepository;
    private final PasswordEncoder passwordEncoder;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final EmailService emailService;

    @PostMapping("/register")
    public String register(@RequestParam String name,
                           @RequestParam String email,
                           @RequestParam String password,
                           @RequestParam String role,
                           @RequestParam(required = false) String skills,
                           RedirectAttributes redirectAttributes) {

        boolean emailTaken = studentRepository.findAll().stream()
                .anyMatch(s -> s.getEmail().equalsIgnoreCase(email));

        if (emailTaken) {
            return "redirect:/register?error";
        }

        Student student = new Student();
        student.setName(name);
        student.setEmail(email);
        student.setPassword(passwordEncoder.encode(password));
        student.setRole(role.toUpperCase());
        student.setSkills(skills);

        studentRepository.save(student);

        return "redirect:/login?registered";
    }

    @PostMapping("/forgot-password")
    public String forgotPassword(@RequestParam String email,
                                 HttpServletRequest request,
                                 Model model) {

        Optional<Student> studentOpt = studentRepository.findAll().stream()
                .filter(s -> s.getEmail().equalsIgnoreCase(email))
                .findFirst();

        if (studentOpt.isPresent()) {
            Student student = studentOpt.get();

            PasswordResetToken resetToken = new PasswordResetToken();
            resetToken.setToken(UUID.randomUUID().toString());
            resetToken.setStudent(student);
            resetToken.setExpiryDate(LocalDateTime.now().plusMinutes(30));
            passwordResetTokenRepository.save(resetToken);

            String resetLink = ServletUriComponentsBuilder.fromRequestUri(request)
                    .replacePath("/reset-password")
                    .replaceQuery("token=" + resetToken.getToken())
                    .build()
                    .toUriString();

            emailService.sendPasswordResetEmail(student.getEmail(), resetLink);
        }

        model.addAttribute("message", "If an account exists with that email, a reset link has been sent.");
        return "forgot-password";
    }

    @GetMapping("/reset-password")
    public String showResetPasswordForm(@RequestParam(required = false) String token, Model model) {
        model.addAttribute("token", token);

        if (token == null) {
            model.addAttribute("error", "Missing reset token.");
            return "reset-password";
        }

        Optional<PasswordResetToken> tokenOpt = passwordResetTokenRepository.findByToken(token);

        if (tokenOpt.isEmpty() || tokenOpt.get().isExpired()) {
            model.addAttribute("error", "This reset link is invalid or has expired.");
        }

        return "reset-password";
    }

    @PostMapping("/reset-password")
    public String resetPassword(@RequestParam String token,
                                @RequestParam String password,
                                Model model) {

        Optional<PasswordResetToken> tokenOpt = passwordResetTokenRepository.findByToken(token);

        if (tokenOpt.isEmpty() || tokenOpt.get().isExpired()) {
            model.addAttribute("token", token);
            model.addAttribute("error", "This reset link is invalid or has expired.");
            return "reset-password";
        }

        PasswordResetToken resetToken = tokenOpt.get();
        Student student = resetToken.getStudent();
        student.setPassword(passwordEncoder.encode(password));
        studentRepository.save(student);

        passwordResetTokenRepository.delete(resetToken);

        model.addAttribute("success", "Your password has been reset. You can now log in.");
        return "reset-password";
    }
}