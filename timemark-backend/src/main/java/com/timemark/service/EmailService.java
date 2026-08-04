package com.timemark.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${app.notifications.enabled}")
    private boolean notificationsEnabled;

    @Value("${spring.mail.host}")
    private String smtpHost;

    @Value("${spring.mail.username}")
    private String fromAddress;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    /**
     * Sends an email if notifications are enabled AND SMTP is actually configured.
     * Otherwise logs what *would* have been sent — so the rest of the app (leave
     * approvals, reminders) never breaks just because email isn't set up yet.
     */
    public void send(String to, String subject, String body) {
        if (!notificationsEnabled || smtpHost == null || smtpHost.isBlank()) {
            log.info("[notifications disabled/unconfigured] Would have emailed {} — subject: \"{}\"", to, subject);
            return;
        }

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(to);
            message.setSubject(subject);
            message.setText(body);
            if (fromAddress != null && !fromAddress.isBlank()) {
                message.setFrom(fromAddress);
            }
            mailSender.send(message);
        } catch (Exception e) {
            // Never let a notification failure break the actual business action (e.g. leave approval)
            log.warn("Failed to send email to {}: {}", to, e.getMessage());
        }
    }
}
