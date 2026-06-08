package com.majstornaklik.service;

import com.majstornaklik.dto.EmailVerificationResponse;
import com.majstornaklik.entity.CompanyRegistrationRequest;
import com.majstornaklik.entity.EmailVerificationToken;
import com.majstornaklik.entity.Handyman;
import com.majstornaklik.entity.User;
import com.majstornaklik.repository.CompanyRegistrationRepository;
import com.majstornaklik.repository.EmailVerificationTokenRepository;
import com.majstornaklik.repository.HandymanRepository;
import com.majstornaklik.repository.UserRepository;
import com.majstornaklik.dto.ResendVerificationResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailVerificationService {

    public static final String ROLE_CLIENT = "ROLE_CLIENT";
    public static final String ROLE_HANDYMAN = "ROLE_HANDYMAN";
    public static final String ROLE_COMPANY = "ROLE_COMPANY";

    private static final long EXPIRY_SECONDS = 86400;
    private static final String STALE_MSG =
            "Link više nije aktuelan. Otvorite najnoviji email ili zatražite novi link.";

    private final EmailVerificationTokenRepository tokenRepository;
    private final UserRepository userRepository;
    private final HandymanRepository handymanRepository;
    private final CompanyRegistrationRepository companyRegistrationRepository;
    private final EmailService emailService;

    @Value("${app.frontend.url:http://localhost:3000}")
    private String frontendUrl;

    @Transactional
    public void sendVerificationEmail(String email, String role, UUID referenceId) {
        String normalized = email.trim().toLowerCase();
        tokenRepository.invalidateUnusedForEmail(normalized);

        String token = UUID.randomUUID().toString();
        EmailVerificationToken verificationToken = EmailVerificationToken.builder()
                .email(normalized)
                .token(token)
                .role(role)
                .referenceId(referenceId)
                .expiresAt(Instant.now().plusSeconds(EXPIRY_SECONDS))
                .build();
        tokenRepository.save(verificationToken);

        String link = frontendUrl + "/verify-email?token=" + token;
        String body = switch (role) {
            case ROLE_CLIENT -> "Poštovani,\n\nHvala na registraciji na Majstor na klik.\n\n"
                    + "Kliknite na link da potvrdite email i aktivirate nalog:\n" + link
                    + "\n\nLink važi 24 sata.\n\nMajstor na klik";
            case ROLE_HANDYMAN -> "Poštovani,\n\nHvala na registraciji majstora na Majstor na klik.\n\n"
                    + "Kliknite na link da potvrdite email i aktivirate nalog:\n" + link
                    + "\n\nLink važi 24 sata.\n\nMajstor na klik";
            case ROLE_COMPANY -> "Poštovani,\n\nHvala na prijavi preduzeća na Majstor na klik.\n\n"
                    + "Kliknite na link da potvrdite email:\n" + link
                    + "\n\nNakon potvrde, admin će pregledati vašu prijavu.\nLink važi 24 sata.\n\nMajstor na klik";
            default -> "Potvrdite email klikom na link:\n" + link;
        };

        try {
            emailService.send(normalized, "Potvrdite email — Majstor na klik", body);
            log.info("[VERIFY] Email poslat na {} | {}", normalized, link);
        } catch (Exception e) {
            log.error("[VERIFY] Greška pri slanju na {}: {}", normalized, e.getMessage());
            log.info("[VERIFY] Link sačuvan u bazi (resend ili ručno): {}", link);
        }
    }

    @Transactional
    public EmailVerificationResponse verify(String token) {
        EmailVerificationToken verificationToken = tokenRepository.findByTokenAndUsedFalse(token)
                .orElse(null);

        if (verificationToken != null) {
            return verifyActiveToken(verificationToken);
        }

        EmailVerificationToken usedToken = tokenRepository.findByToken(token).orElse(null);
        if (usedToken != null) {
            return resolveUsedToken(usedToken);
        }

        return EmailVerificationResponse.of("STALE_LINK", STALE_MSG);
    }

    private EmailVerificationResponse verifyActiveToken(EmailVerificationToken verificationToken) {
        if (verificationToken.getExpiresAt().isBefore(Instant.now())) {
            return EmailVerificationResponse.of("EXPIRED",
                    "Link za verifikaciju je istekao. Registrujte se ponovo ili zatražite novi link.");
        }

        String role = verificationToken.getRole();
        String email = verificationToken.getEmail();

        return switch (role) {
            case ROLE_CLIENT -> verifyClient(email, verificationToken);
            case ROLE_HANDYMAN -> verifyHandyman(email, verificationToken);
            case ROLE_COMPANY -> verifyCompany(verificationToken);
            default -> EmailVerificationResponse.of("INVALID", "Nepoznat tip verifikacije.");
        };
    }

    private EmailVerificationResponse resolveUsedToken(EmailVerificationToken verificationToken) {
        String role = verificationToken.getRole();
        String email = verificationToken.getEmail();

        return switch (role) {
            case ROLE_CLIENT -> userRepository.findByEmail(email)
                    .filter(u -> Boolean.TRUE.equals(u.getEmailVerified()))
                    .map(u -> EmailVerificationResponse.of("ALREADY_VERIFIED",
                            "Email je već potvrđen. Možete se prijaviti."))
                    .orElse(EmailVerificationResponse.of("STALE_LINK", STALE_MSG));
            case ROLE_HANDYMAN -> handymanRepository.findByEmail(email)
                    .filter(h -> Boolean.TRUE.equals(h.getEmailVerified()))
                    .map(h -> EmailVerificationResponse.of("ALREADY_VERIFIED",
                            "Email je već potvrđen. Možete se prijaviti."))
                    .orElse(EmailVerificationResponse.of("STALE_LINK", STALE_MSG));
            case ROLE_COMPANY -> companyRegistrationRepository.findById(verificationToken.getReferenceId())
                    .filter(req -> Boolean.TRUE.equals(req.getEmailVerified()))
                    .map(req -> EmailVerificationResponse.of("ALREADY_VERIFIED",
                            "Email je već potvrđen. Admin će pregledati vašu prijavu."))
                    .orElse(EmailVerificationResponse.of("STALE_LINK", STALE_MSG));
            default -> EmailVerificationResponse.of("INVALID", "Nepoznat tip verifikacije.");
        };
    }

    @Transactional
    public ResendVerificationResponse resend(String email) {
        String normalized = email.trim().toLowerCase();

        if (userRepository.findByEmail(normalized).filter(u -> !Boolean.TRUE.equals(u.getEmailVerified())).isPresent()) {
            sendVerificationEmail(normalized, ROLE_CLIENT, null);
            return ResendVerificationResponse.dispatched();
        }
        if (handymanRepository.findByEmail(normalized).filter(h -> !Boolean.TRUE.equals(h.getEmailVerified())).isPresent()) {
            sendVerificationEmail(normalized, ROLE_HANDYMAN, null);
            return ResendVerificationResponse.dispatched();
        }
        var companyPending = companyRegistrationRepository
                .findTopByEmailAndStatusAndEmailVerifiedFalseOrderByCreatedAtDesc(normalized, "PENDING");
        if (companyPending.isPresent()) {
            sendVerificationEmail(normalized, ROLE_COMPANY, companyPending.get().getId());
            return ResendVerificationResponse.dispatched();
        }

        log.warn("[VERIFY] Resend: nalog nije pronađen ili je već potvrđen ({})", normalized);
        return ResendVerificationResponse.notFound();
    }

    private EmailVerificationResponse verifyClient(String email, EmailVerificationToken token) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Korisnik nije pronađen"));
        if (Boolean.TRUE.equals(user.getEmailVerified())) {
            markUsed(token);
            return EmailVerificationResponse.of("ALREADY_VERIFIED", "Email je već potvrđen. Možete se prijaviti.");
        }
        user.setEmailVerified(true);
        userRepository.save(user);
        markUsed(token);
        return EmailVerificationResponse.of("VERIFIED",
                "Email je uspešno potvrđen. Sada se možete prijaviti na nalog.");
    }

    private EmailVerificationResponse verifyHandyman(String email, EmailVerificationToken token) {
        Handyman handyman = handymanRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Majstor nije pronađen"));
        if (Boolean.TRUE.equals(handyman.getEmailVerified())) {
            markUsed(token);
            return EmailVerificationResponse.of("ALREADY_VERIFIED", "Email je već potvrđen. Možete se prijaviti.");
        }
        handyman.setEmailVerified(true);
        handymanRepository.save(handyman);
        markUsed(token);
        return EmailVerificationResponse.of("VERIFIED",
                "Email je uspešno potvrđen. Sada se možete prijaviti na nalog.");
    }

    private EmailVerificationResponse verifyCompany(EmailVerificationToken token) {
        CompanyRegistrationRequest req = companyRegistrationRepository.findById(token.getReferenceId())
                .orElseThrow(() -> new IllegalArgumentException("Prijava nije pronađena"));

        if (Boolean.TRUE.equals(req.getEmailVerified())) {
            markUsed(token);
            return EmailVerificationResponse.of("ALREADY_VERIFIED",
                    "Email je već potvrđen. Admin će pregledati vašu prijavu.");
        }

        req.setEmailVerified(true);
        companyRegistrationRepository.save(req);
        markUsed(token);

        emailService.sendToAdmin("Nova registracija preduzeća (email potvrđen)",
                "Preduzeće: " + req.getCompanyName() + "\nEmail: " + req.getEmail()
                        + "\nKontakt: " + req.getContactPerson() + "\nTelefon: " + req.getNormalizedPhone());

        emailService.send(req.getEmail(), "Email potvrđen — prijava preduzeća",
                "Poštovani,\n\nVaš email je potvrđen.\nAdmin će pregledati prijavu preduzeća \""
                        + req.getCompanyName() + "\" i obavestićemo vas o odluci.\n\nMajstor na klik");

        return EmailVerificationResponse.of("VERIFIED",
                "Email je potvrđen. Admin će pregledati prijavu preduzeća i obavestićemo vas emailom.");
    }

    private void markUsed(EmailVerificationToken token) {
        token.setUsed(true);
        tokenRepository.save(token);
    }
}
