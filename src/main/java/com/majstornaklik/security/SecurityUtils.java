package com.majstornaklik.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class SecurityUtils {

    public UserPrincipal getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof UserPrincipal principal)) {
            throw new IllegalStateException("Korisnik nije autentifikovan");
        }
        return principal;
    }

    public UserPrincipal getCurrentUserOrNull() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof UserPrincipal principal)) {
            return null;
        }
        return principal;
    }

    public UUID getCurrentUserId() {
        return getCurrentUser().getId();
    }

    public String getCurrentRole() {
        return getCurrentUser().getRole();
    }

    public void requireRole(String role) {
        if (!role.equals(getCurrentRole())) {
            throw new IllegalStateException("Nedozvoljen pristup");
        }
    }
}
