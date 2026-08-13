package com.library.smart_internship.service;

import lombok.RequiredArgsConstructor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    @Retryable(maxAttempts = 3, backoff = @Backoff(delay = 2000, multiplier = 2))
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

    @Retryable(maxAttempts = 3, backoff = @Backoff(delay = 2000, multiplier = 2))
    public void sendNewApplicationEmail(String recruiterEmail, String studentName, String programTitle) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom("noreply@smartinternship.com");
        message.setTo(recruiterEmail);
        message.setSubject("New Application Received: " + programTitle);

        String emailBody = String.format(
                "Hello,\n\n" +
                        "%s has just applied to your '%s' program.\n\n" +
                        "Log in to your recruiter dashboard to review the application.\n\n" +
                        "Best regards,\nSmart Internship Platform Team",
                studentName, programTitle
        );

        message.setText(emailBody);
        mailSender.send(message);
    }

    @Retryable(maxAttempts = 3, backoff = @Backoff(delay = 2000, multiplier = 2))
    public void sendPasswordResetEmail(String toEmail, String resetLink) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom("noreply@smartinternship.com");
        message.setTo(toEmail);
        message.setSubject("Reset Your Password - Smart Internship Platform");

        String emailBody = String.format(
                "Hello,\n\n" +
                        "We received a request to reset your password.\n\n" +
                        "Click the link below to set a new password (valid for 30 minutes):\n%s\n\n" +
                        "If you did not request this, you can safely ignore this email.\n\n" +
                        "Best regards,\nSmart Internship Platform Team",
                resetLink
        );

        message.setText(emailBody);
        mailSender.send(message);
    }
}