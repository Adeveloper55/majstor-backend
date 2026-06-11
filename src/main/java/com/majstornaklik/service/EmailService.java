package com.majstornaklik.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.internet.MimeMessage;

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
        sendInternal(to, subject, body, true);
    }

    /** Ne prekida transakciju ako SMTP ne uspe (npr. admin kreira nalog). */
    public void sendSafely(String to, String subject, String body) {
        sendInternal(to, subject, body, false);
    }

    public void sendHtmlSafely(String to, String subject, String htmlBody) {
        sendHtmlInternal(to, subject, htmlBody, null, null, null, false);
    }

    public void sendHtmlWithInlineImageSafely(
            String to,
            String subject,
            String htmlBody,
            String inlineCid,
            byte[] imageBytes,
            String imageMimeType
    ) {
        sendHtmlInternal(to, subject, htmlBody, inlineCid, imageBytes, imageMimeType, false);
    }

    public void sendToAdmin(String subject, String body) {
        sendSafely(adminEmail, subject, body);
    }

    private void sendInternal(String to, String subject, String body, boolean failOnError) {
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
            if (failOnError) {
                throw e;
            }
        }
    }

    private void sendHtmlInternal(
            String to,
            String subject,
            String htmlBody,
            String inlineCid,
            byte[] imageBytes,
            String imageMimeType,
            boolean failOnError
    ) {
        if (fromEmail == null || fromEmail.isBlank()) {
            log.info("[EMAIL HTML] To: {} | Subject: {} | HTML length: {}", to, subject, htmlBody.length());
            return;
        }
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(appFrom);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlBody, true);
            if (inlineCid != null && imageBytes != null && imageBytes.length > 0) {
                helper.addInline(inlineCid, new ByteArrayResource(imageBytes), imageMimeType != null ? imageMimeType : "image/png");
            }
            mailSender.send(message);
            log.info("[EMAIL HTML] Poslato: {} | {}", to, subject);
        } catch (Exception e) {
            log.error("[EMAIL HTML] Greška pri slanju na {}: {}", to, e.getMessage());
            if (failOnError) {
                throw new IllegalStateException("Greška pri slanju emaila: " + e.getMessage(), e);
            }
        }
    }
}
