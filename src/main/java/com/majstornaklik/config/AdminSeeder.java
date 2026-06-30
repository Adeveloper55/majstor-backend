package com.majstornaklik.config;

import com.majstornaklik.entity.Admin;
import com.majstornaklik.repository.AdminRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class AdminSeeder implements CommandLineRunner {

    private static final String DEV_DEFAULT_PASSWORD = "admin123";

    private final AdminRepository adminRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.admin.seed-enabled:true}")
    private boolean seedEnabled;

    @Value("${app.admin.email:admin@majstornaklik.rs}")
    private String adminEmail;

    @Value("${app.admin.password:}")
    private String adminPassword;

    @Value("${app.admin.reset-password-on-start:false}")
    private boolean resetPasswordOnStart;

    @Override
    public void run(String... args) {
        if (!seedEnabled) {
            return;
        }

        String email = adminEmail.trim().toLowerCase();
        String password = resolvePassword();

        adminRepository.findByEmail(email).ifPresentOrElse(admin -> {
            if (resetPasswordOnStart) {
                admin.setPasswordHash(passwordEncoder.encode(password));
                adminRepository.save(admin);
                log.info("Admin lozinka ažurirana ({})", email);
            }
        }, () -> {
            Admin admin = Admin.builder()
                    .fullName("Admin")
                    .email(email)
                    .passwordHash(passwordEncoder.encode(password))
                    .isActive(true)
                    .build();
            adminRepository.save(admin);
            log.info("Admin kreiran ({})", email);
        });
    }

    private String resolvePassword() {
        if (adminPassword != null && !adminPassword.isBlank()) {
            return adminPassword;
        }
        log.warn("APP_ADMIN_PASSWORD nije postavljen — koristi se dev lozinka. Postavi jak password na produkciji!");
        return DEV_DEFAULT_PASSWORD;
    }
}
