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
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.security.Principal;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/student")
@RequiredArgsConstructor
public class StudentController {

    private final InternshipRepository internshipRepository;
    private final StudentRepository studentRepository;
    private final ApplicationRepository applicationRepository;
    private final EmailService emailService;

    @GetMapping("/dashboard")
    public String studentDashboard(@RequestParam(value = "keyword", required = false) String keyword,
                                   @RequestParam(value = "category", required = false) String category,
                                   Principal principal,
                                   Model model) {
        Student student = getStudent(principal);

        List<Internship> availableInternships;
        if (keyword != null && !keyword.trim().isEmpty()) {
            availableInternships = internshipRepository
                    .findByTitleContainingIgnoreCaseOrSkillsRequiredContainingIgnoreCase(keyword, keyword);
        } else {
            availableInternships = internshipRepository.findAll();
        }

        availableInternships = availableInternships.stream()
                .filter(Internship::isActive)
                .filter(program -> !program.isExpired())
                .filter(program -> category == null || category.isEmpty() || category.equals(program.getCategory()))
                .collect(Collectors.toList());

        List<Application> myApplications = applicationRepository.findByStudentId(student.getId());

        Set<Long> appliedInternshipIds = myApplications.stream()
                .map(app -> app.getInternship().getId())
                .collect(Collectors.toSet());

        model.addAttribute("student", student);
        model.addAttribute("internships", availableInternships);
        model.addAttribute("myApplications", myApplications);
        model.addAttribute("appliedInternshipIds", appliedInternshipIds);
        model.addAttribute("keyword", keyword);
        model.addAttribute("selectedCategory", category);

        return "dashboard";
    }

    @PostMapping("/submit-application")
    public String applyToInternship(@RequestParam("internshipId") Long internshipId,
                                    @RequestParam("applicantName") String applicantName,
                                    @RequestParam("applicantAge") Integer applicantAge,
                                    @RequestParam("applicantPhone") String applicantPhone,
                                    @RequestParam(value = "coverLetter", required = false) String coverLetter,
                                    @RequestParam(value = "file", required = false) MultipartFile file,
                                    Principal principal,
                                    RedirectAttributes redirectAttributes) throws IOException {
        Student student = getStudent(principal);

        Internship internship = internshipRepository.findById(internshipId)
                .orElseThrow(() -> new RuntimeException("Internship not found"));

        boolean alreadyApplied = applicationRepository.findByStudentId(student.getId()).stream()
                .anyMatch(app -> app.getInternship().getId().equals(internshipId));

        if (alreadyApplied) {
            redirectAttributes.addFlashAttribute("error", "You have already applied to this program.");
            return "redirect:/student/dashboard";
        }

        if (internship.isExpired()) {
            redirectAttributes.addFlashAttribute("error", "This program's application deadline has passed.");
            return "redirect:/student/dashboard";
        }

        Application application = new Application();
        application.setStudent(student);
        application.setInternship(internship);
        application.setStatus("PENDING");
        application.setApplicantName(applicantName);
        application.setApplicantAge(applicantAge);
        application.setApplicantPhone(applicantPhone);
        application.setCoverLetter(coverLetter);

        if (file != null && !file.isEmpty()) {
            application.setResumeData(file.getBytes());
            application.setResumeFilename(file.getOriginalFilename());
            application.setResumeContentType(file.getContentType());
        }

        applicationRepository.save(application);

        if (internship.getRecruiter() != null && internship.getRecruiter().getEmail() != null) {
            try {
                emailService.sendNewApplicationEmail(
                        internship.getRecruiter().getEmail(),
                        applicantName,
                        internship.getTitle()
                );
            } catch (Exception e) {
                System.err.println("New application email failed: " + e.getMessage());
            }
        }

        redirectAttributes.addFlashAttribute("success", "Application submitted successfully!");

        return "redirect:/student/dashboard";
    }

    @PostMapping("/applications/{id}/withdraw")
    public String withdrawApplication(@PathVariable("id") Long applicationId,
                                      Principal principal,
                                      RedirectAttributes redirectAttributes) {
        Student student = getStudent(principal);

        Application application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new RuntimeException("Application not found"));

        if (!application.getStudent().getId().equals(student.getId())) {
            redirectAttributes.addFlashAttribute("error", "You cannot withdraw this application.");
            return "redirect:/student/dashboard";
        }

        if (!"PENDING".equalsIgnoreCase(application.getStatus())) {
            redirectAttributes.addFlashAttribute("error", "Only pending applications can be withdrawn.");
            return "redirect:/student/dashboard";
        }

        application.setStatus("WITHDRAWN");
        applicationRepository.save(application);

        redirectAttributes.addFlashAttribute("success", "Application withdrawn. You can reapply to this program if you change your mind.");
        return "redirect:/student/dashboard";
    }

    @GetMapping("/profile")
    public String viewProfile(Principal principal, Model model) {
        Student student = getStudent(principal);
        model.addAttribute("student", student);
        return "profile";
    }

    @PostMapping("/profile/update")
    public String updateProfile(@RequestParam("name") String name,
                                @RequestParam("phone") String phone,
                                @RequestParam(value = "skills", required = false) String skills,
                                Principal principal,
                                RedirectAttributes redirectAttributes) {
        Student student = getStudent(principal);
        student.setName(name);
        student.setPhone(phone);
        student.setSkills(skills);
        studentRepository.save(student);

        redirectAttributes.addFlashAttribute("success", "Profile updated successfully!");
        return "redirect:/student/profile";
    }

    private Student getStudent(Principal principal) {
        String email = principal.getName();
        return studentRepository.findAll().stream()
                .filter(s -> s.getEmail().equals(email))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Student account not found"));
    }
}