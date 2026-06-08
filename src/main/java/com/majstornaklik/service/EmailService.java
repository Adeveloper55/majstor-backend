package com.majstornaklik.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username:}")
    private String fromEmail;

    @Value("${app.mail.from:contact@tvojmajstornaklik.com}")
    private String appFrom;

    @Value("${app.mail.admin:contact@tvojmajstornaklik.com}")
    private String adminEmail;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void send(String to, String subject, String body) {
        if (fromEmail == null || fromEmail.isBlank()) {
            log.info("[EMAIL] To: {} | Subject: {} | Body: {}", to, subject, body);
            return;
        }
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(appFrom);
        message.setTo(to);
        message.setSubject(subject);
        message.setText(body);
        try {
            mailSender.send(message);
            log.info("[EMAIL] Poslato: {} | {}", to, subject);
        } catch (Exception e) {
            log.error("[EMAIL] Greška pri slanju na {}: {}", to, e.getMessage());
            throw e;
        }
    }

    public void sendToAdmin(String subject, String body) {
        send(adminEmail, subject, body);
    }
}
