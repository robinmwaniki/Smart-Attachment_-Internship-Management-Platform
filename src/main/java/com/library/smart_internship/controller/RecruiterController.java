package com.library.smart_internship.controller;

import com.library.smart_internship.entity.Application;
import com.library.smart_internship.entity.Internship;
import com.library.smart_internship.entity.Student;
import com.library.smart_internship.repository.ApplicationRepository;
import com.library.smart_internship.repository.InternshipRepository;
import com.library.smart_internship.repository.StudentRepository;
import com.library.smart_internship.service.EmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@Controller
@RequestMapping("/recruiter")
@RequiredArgsConstructor
public class RecruiterController {

    private final InternshipRepository internshipRepository;
    private final StudentRepository studentRepository;
    private final ApplicationRepository applicationRepository;
    private final EmailService emailService;

    @GetMapping("/dashboard")
    public String recruiterDashboard(Principal principal, Model model) {
        String email = principal.getName();
        Student recruiter = studentRepository.findAll().stream()
                .filter(s -> s.getEmail().equals(email))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Recruiter account not found"));

        List<Internship> myPrograms = internshipRepository.findByRecruiterId(recruiter.getId());

        model.addAttribute("recruiter", recruiter);
        model.addAttribute("programs", myPrograms);
        model.addAttribute("newInternship", new Internship());

        List<Application> myApplications = applicationRepository.findByInternshipRecruiterId(recruiter.getId());
        model.addAttribute("applications", myApplications);

        return "recruiter-dashboard";
    }

    @PostMapping("/internships/post")
    public String createProgram(@ModelAttribute("newInternship") Internship internship, Principal principal) {
        String email = principal.getName();
        Student recruiter = studentRepository.findAll().stream()
                .filter(s -> s.getEmail().equals(email))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Recruiter account not found"));

        internship.setRecruiter(recruiter);
        internshipRepository.save(internship);
        return "redirect:/recruiter/dashboard";
    }

    @PostMapping("/applications/{id}/review")
    public String reviewApplication(@PathVariable("id") Long applicationId,
                                    @RequestParam("status") String status,
                                    @RequestParam("feedback") String feedback) {

        Application application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new RuntimeException("Application not found"));

        application.setStatus(status);
        application.setFeedback(feedback);
        applicationRepository.save(application);

        try {
            emailService.sendStatusUpdateEmail(
                    application.getStudent().getEmail(),
                    application.getStudent().getName(),
                    application.getInternship().getTitle(),
                    status,
                    feedback
            );
        } catch (Exception e) {
            System.err.println("Email failed to dispatch: " + e.getMessage());
        }

        return "redirect:/recruiter/dashboard";
    }
}