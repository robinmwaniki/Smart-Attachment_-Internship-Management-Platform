package com.library.smart_internship.controller;

import com.library.smart_internship.entity.Application;
import com.library.smart_internship.entity.Internship;
import com.library.smart_internship.entity.Student;
import com.library.smart_internship.repository.ApplicationRepository;
import com.library.smart_internship.repository.InternshipRepository;
import com.library.smart_internship.repository.StudentRepository;
import com.library.smart_internship.service.EmailService;
import com.library.smart_internship.service.SmsService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/recruiter")
@RequiredArgsConstructor
public class RecruiterController {

    private final InternshipRepository internshipRepository;
    private final StudentRepository studentRepository;
    private final ApplicationRepository applicationRepository;
    private final EmailService emailService;
    private final SmsService smsService;

    /**
     * Get recruiter from authentication
     */
    private Student getRecruiterFromAuth(Authentication authentication) {
        String email = authentication.getName();
        return studentRepository.findAll().stream()
                .filter(s -> s.getEmail().equals(email))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Recruiter account not found"));
    }

    @GetMapping("/dashboard")
    public String recruiterDashboard(Authentication authentication, Model model) {
        Student recruiter = getRecruiterFromAuth(authentication);

        List<Internship> myPrograms = internshipRepository.findByRecruiterId(recruiter.getId());

        model.addAttribute("recruiter", recruiter);
        model.addAttribute("programs", myPrograms);
        model.addAttribute("newInternship", new Internship());

        List<Application> myApplications = applicationRepository.findByInternshipRecruiterId(recruiter.getId());
        model.addAttribute("applications", myApplications);

        return "recruiter-dashboard";
    }

    @PostMapping("/internships/post")
    public String createProgram(@ModelAttribute("newInternship") Internship internship, Authentication authentication) {
        Student recruiter = getRecruiterFromAuth(authentication);

        internship.setRecruiter(recruiter);
        internshipRepository.save(internship);
        return "redirect:/recruiter/dashboard";
    }

    @PostMapping("/applications/{id}/review")
    public String reviewApplication(@PathVariable("id") Long applicationId,
                                    @RequestParam("status") String status,
                                    @RequestParam("feedback") String feedback,
                                    RedirectAttributes redirectAttributes) {

        Application application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new RuntimeException("Application not found"));

        if (isFinalized(application.getStatus())) {
            redirectAttributes.addFlashAttribute("error",
                    "This application was already " + application.getStatus().toLowerCase() + " and cannot be changed.");
            return "redirect:/recruiter/dashboard";
        }

        processReview(application, status, feedback);

        return "redirect:/recruiter/dashboard";
    }

    @PostMapping("/applications/bulk-review")
    public String bulkReviewApplications(@RequestParam("applicationIds") List<Long> applicationIds,
                                         @RequestParam("status") String status,
                                         @RequestParam(value = "feedback", required = false) String feedback,
                                         RedirectAttributes redirectAttributes) {

        String effectiveFeedback = (feedback == null || feedback.isBlank())
                ? ("APPROVED".equalsIgnoreCase(status) ? "Your application has been approved." : "Your application was not successful this time.")
                : feedback;

        int skipped = 0;
        for (Long applicationId : applicationIds) {
            Application application = applicationRepository.findById(applicationId).orElse(null);
            if (application == null) {
                continue;
            }
            if (isFinalized(application.getStatus())) {
                skipped++;
                continue;
            }
            processReview(application, status, effectiveFeedback);
        }

        if (skipped > 0) {
            redirectAttributes.addFlashAttribute("error",
                    skipped + " application(s) were skipped because they were already reviewed.");
        }

        return "redirect:/recruiter/dashboard";
    }

    private boolean isFinalized(String status) {
        return "APPROVED".equalsIgnoreCase(status) || "REJECTED".equalsIgnoreCase(status);
    }

    private void processReview(Application application, String status, String feedback) {
        String previousStatus = application.getStatus();

        application.setStatus(status);
        application.setFeedback(feedback);
        applicationRepository.save(application);

        adjustSlotsForStatusChange(application.getInternship(), previousStatus, status);

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

        try {
            String smsText = String.format(
                    "Hi %s, your application for %s is now %s. Check your dashboard for details.",
                    application.getStudent().getName(),
                    application.getInternship().getTitle(),
                    status
            );
            smsService.sendSms(application.getStudent().getPhone(), smsText);
        } catch (Exception e) {
            System.err.println("SMS failed to dispatch: " + e.getMessage());
        }
    }

    private void adjustSlotsForStatusChange(Internship internship, String previousStatus, String newStatus) {
        if (internship.getSlotsAvailable() == null) {
            return;
        }

        boolean wasApproved = "APPROVED".equalsIgnoreCase(previousStatus);
        boolean isApproved = "APPROVED".equalsIgnoreCase(newStatus);

        if (!wasApproved && isApproved) {
            int remaining = Math.max(internship.getSlotsAvailable() - 1, 0);
            internship.setSlotsAvailable(remaining);
        } else if (wasApproved && !isApproved) {
            internship.setSlotsAvailable(internship.getSlotsAvailable() + 1);
        }

        internshipRepository.save(internship);
    }
}