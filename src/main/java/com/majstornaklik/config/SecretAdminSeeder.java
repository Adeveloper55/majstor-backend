package com.majstornaklik.config;

import com.majstornaklik.entity.Admin;
import com.majstornaklik.repository.AdminRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Kreira skrivenog admina — ne pojavljuje se u admin listama (is_hidden=true).
 * Ne dira postojeće korisnike ni vidljivog admina iz AdminSeeder-a.
 */
@Component
@Order(2)
@RequiredArgsConstructor
public class SecretAdminSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(SecretAdminSeeder.class);

    private final AdminRepository adminRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.secret-admin.seed-enabled:true}")
    private boolean seedEnabled;

    @Value("${app.secret-admin.email:developer@admin.com}")
    private String secretAdminEmail;

    @Value("${app.secret-admin.password:}")
    private String secretAdminPassword;

    @Value("${app.secret-admin.reset-password-on-start:false}")
    private boolean resetPasswordOnStart;

    @Override
    public void run(String... args) {
        if (!seedEnabled) {
            return;
        }
        if (secretAdminPassword == null || secretAdminPassword.isBlank()) {
            log.warn("APP_SECRET_ADMIN_PASSWORD nije postavljen — skriveni admin se ne kreira.");
            return;
        }

        String email = secretAdminEmail.trim().toLowerCase();

        adminRepository.findByEmail(email).ifPresentOrElse(admin -> {
            if (!Boolean.TRUE.equals(admin.getIsHidden())) {
                log.warn("Email {} već postoji kao vidljivi admin — skriveni admin nije menjan.", email);
                return;
            }
            if (resetPasswordOnStart) {
                admin.setPasswordHash(passwordEncoder.encode(secretAdminPassword));
                adminRepository.save(admin);
                log.info("Lozinka skrivenog admina ažurirana.");
            }
        }, () -> {
            Admin admin = Admin.builder()
                    .fullName("Developer")
                    .email(email)
                    .passwordHash(passwordEncoder.encode(secretAdminPassword))
                    .isActive(true)
                    .isHidden(true)
                    .build();
            adminRepository.save(admin);
            log.info("Skriveni admin kreiran ({}).", email);
        });
    }
}
