package com.majstornaklik.config;

import com.majstornaklik.entity.Admin;
import com.majstornaklik.repository.AdminRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class AdminSeeder implements CommandLineRunner {

    private final AdminRepository adminRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        adminRepository.findByEmail("admin@majstornaklik.rs").ifPresentOrElse(admin -> {
            admin.setPasswordHash(passwordEncoder.encode("admin123"));
            adminRepository.save(admin);
            log.info("Admin lozinka ažurirana (admin@majstornaklik.rs / admin123)");
        }, () -> {
            Admin admin = Admin.builder()
                    .fullName("Admin")
                    .email("admin@majstornaklik.rs")
                    .passwordHash(passwordEncoder.encode("admin123"))
                    .isActive(true)
                    .build();
            adminRepository.save(admin);
            log.info("Admin kreiran (admin@majstornaklik.rs / admin123)");
        });
    }
}
