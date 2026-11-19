package com.library.smart_internship.service;

import lombok.RequiredArgsConstructor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
/**
 * SMTP calls are guarded by a circuit breaker and bounded semaphore. Sending an
 * email is not idempotent, so an ambiguous timeout must not trigger an automatic
 * retry that could deliver the same notification more than once.
 */
public class EmailService {

    private final JavaMailSender mailSender;

    @CircuitBreaker(name = "emailGateway")
    @Bulkhead(name = "emailGateway")
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

    @CircuitBreaker(name = "emailGateway")
    @Bulkhead(name = "emailGateway")
    public void sendNewApplicationEmail(String recruiterEmail, String studentName, String programTitle) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom("noreply@smartinternship.com");
        message.setTo(recruiterEmail);
        message.setSubject("New Application Received: " + programTitle);

        String emailBody = String.format(
                """
                        Hello,
                        
                        %s has just applied to your '%s' program.
                        
                        Log in to your recruiter dashboard to review the application.
                        
                        Best regards,
                        Smart Internship Platform Team""",
                studentName, programTitle
        );

        message.setText(emailBody);
        mailSender.send(message);
    }

    @CircuitBreaker(name = "emailGateway")
    @Bulkhead(name = "emailGateway")
    public void sendPasswordResetEmail(String toEmail, String resetLink) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom("noreply@smartinternship.com");
        message.setTo(toEmail);
        message.setSubject("Reset Your Password - Smart Internship Platform");

        String emailBody = String.format(
                """
                        Hello,
                        
                        We received a request to reset your password.
                        
                        Click the link below to set a new password (valid for 30 minutes):
                        %s
                        
                        If you did not request this, you can safely ignore this email.
                        
                        Best regards,
                        Smart Internship Platform Team""",
                resetLink
        );

        message.setText(emailBody);
        mailSender.send(message);
    }
}