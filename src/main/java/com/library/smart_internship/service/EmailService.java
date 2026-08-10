package com.library.smart_internship.service;

import lombok.RequiredArgsConstructor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    public void sendStatusUpdateEmail(String toEmail, String studentName, String programTitle, String status, String feedback) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom("noreply@smartinternship.com");
        message.setTo(toEmail);
        message.setSubject("Application Status Update: " + programTitle);

        String emailBody = String.format(
                "Hello %s,\n\n" +
                        "Your application for the '%s' program has been reviewed.\n\n" +
                        "Status: %s\n" +
                        "Recruiter Message / Interview Details:\n%s\n\n" +
                        "Best regards,\nSmart Internship Platform Team",
                studentName, programTitle, status, feedback
        );

        message.setText(emailBody);
        mailSender.send(message);
    }
}