package com.majstornaklik.security;

import com.majstornaklik.repository.AdminRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PrimaryAdminAuthorization {

    private final AdminRepository adminRepository;

    @Value("${app.admin.email:admin@majstornaklik.rs}")
    private String primaryAdminEmail;

    public void requirePrimaryAdmin() {
        if (!isPrimaryAdmin(currentEmail())) {
            throw new AccessDeniedException("Samo glavni admin može pristupiti upitima.");
        }
    }

    public boolean isPrimaryAdmin(String email) {
        if (email == null || email.isBlank()) {
            return false;
        }
        if (!email.trim().equalsIgnoreCase(primaryAdminEmail.trim())) {
            return false;
        }
        return adminRepository.findByEmail(email.trim().toLowerCase())
                .map(admin -> !Boolean.TRUE.equals(admin.getIsHidden()))
                .orElse(false);
    }

    private String currentEmail() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null ? auth.getName() : null;
    }
}
