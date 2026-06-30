package com.majstornaklik;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

@SpringBootTest
class MailSmtpSmokeTest {

    @Autowired
    private JavaMailSender mailSender;

    @Value("${spring.mail.password:}")
    private String mailPassword;

    @Value("${spring.mail.username:}")
    private String mailUsername;

    @Test
    void sendsTestEmailViaNamecheapSmtp() {
        assumeTrue(mailUsername != null && !mailUsername.isBlank(), "spring.mail.username nije podešen");
        assumeTrue(mailPassword != null && !mailPassword.isBlank(), "spring.mail.password nije podešen");

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom("contact@majstor365.com");
        message.setTo("contact@majstor365.com");
        message.setSubject("Majstor 365 SMTP test");
        message.setText("Test poruka — Namecheap Private Email radi.");

        assertDoesNotThrow(() -> mailSender.send(message));
    }
}
