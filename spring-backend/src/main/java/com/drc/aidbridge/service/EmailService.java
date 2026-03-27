package com.drc.aidbridge.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * Email service for sending OTP and notification emails.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username:noreply@aidbridge.com}")
    private String fromEmail;

    @Value("${app.name:AidBridge}")
    private String appName;

    /**
     * Send OTP email for registration verification.
     */
    @Async
    public void sendOtpEmail(String to, String otp) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(to);
            message.setSubject(appName + " - Email Verification Code");
            message.setText(buildOtpEmailBody(otp, "verify your email"));

            mailSender.send(message);
            log.info("OTP email sent to {}", to);
        } catch (Exception e) {
            log.error("Failed to send OTP email to {}: {}", to, e.getMessage());
        }
    }

    /**
     * Send password reset OTP email.
     */
    @Async
    public void sendPasswordResetEmail(String to, String otp) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(to);
            message.setSubject(appName + " - Password Reset Code");
            message.setText(buildOtpEmailBody(otp, "reset your password"));

            mailSender.send(message);
            log.info("Password reset email sent to {}", to);
        } catch (Exception e) {
            log.error("Failed to send password reset email to {}: {}", to, e.getMessage());
        }
    }

    /**
     * Send welcome email after successful registration.
     */
    @Async
    public void sendWelcomeEmail(String to, String userName) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(to);
            message.setSubject("Welcome to " + appName + "!");
            message.setText(String.format(
                    "Hi %s,\n\n" +
                            "Welcome to %s! Your account has been successfully verified.\n\n" +
                            "You can now use the app to:\n" +
                            "- Request aid during emergencies\n" +
                            "- Volunteer to help those in need\n" +
                            "- Donate supplies to relief efforts\n\n" +
                            "Thank you for joining our community.\n\n" +
                            "Best regards,\n" +
                            "The %s Team",
                    userName, appName, appName));

            mailSender.send(message);
            log.info("Welcome email sent to {}", to);
        } catch (Exception e) {
            log.error("Failed to send welcome email to {}: {}", to, e.getMessage());
        }
    }

    private String buildOtpEmailBody(String otp, String action) {
        return String.format(
                "Your %s verification code is:\n\n" +
                        "%s\n\n" +
                        "Use this code to %s. This code will expire in 5 minutes.\n\n" +
                        "If you didn't request this code, please ignore this email.\n\n" +
                        "Best regards,\n" +
                        "The %s Team",
                appName, otp, action, appName);
    }
}
