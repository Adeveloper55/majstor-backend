package com.majstornaklik.service;

import com.majstornaklik.dto.*;
import com.majstornaklik.entity.*;
import com.majstornaklik.repository.*;
import com.majstornaklik.security.CustomUserDetailsService;
import com.majstornaklik.security.JwtService;
import com.majstornaklik.security.PrimaryAdminAuthorization;
import com.majstornaklik.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.majstornaklik.util.PibUtils;
import com.majstornaklik.util.PhoneUtils;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final HandymanRepository handymanRepository;
    private final AdminRepository adminRepository;
    private final PasswordResetTokenRepository resetTokenRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final EmailService emailService;
    private final HandymanCategoryService handymanCategoryService;
    private final EmailVerificationService emailVerificationService;
    private final PhoneUniquenessService phoneUniquenessService;
    private final CustomUserDetailsService userDetailsService;
    private final PrimaryAdminAuthorization primaryAdminAuthorization;

    @Value("${app.frontend.url:http://localhost:3000}")
    private String frontendUrl;

    @Value("${jwt.refresh-expiration:604800000}")
    private long refreshExpirationMs;

    @Transactional
    public RegisterPendingResponse registerClient(RegisterClientRequest req) {
        if (userRepository.existsByEmail(req.email()) || handymanRepository.existsByEmail(req.email())) {
            throw new IllegalArgumentException("Email je već registrovan");
        }
        String phoneNormalized = PhoneUtils.normalizeOptional(req.phone());
        phoneUniquenessService.assertPhoneAvailable(phoneNormalized, null, null, null);
        User user = User.builder()
                .fullName(req.fullName())
                .email(req.email().trim().toLowerCase())
                .passwordHash(passwordEncoder.encode(req.password()))
                .phone(phoneNormalized)
                .phoneNormalized(phoneNormalized)
                .city(req.city())
                .emailVerified(false)
                .build();
        userRepository.save(user);
        emailVerificationService.sendVerificationEmail(user.getEmail(), EmailVerificationService.ROLE_CLIENT, null);
        return new RegisterPendingResponse(
                "Registracija uspešna. Proverite email i kliknite na link za potvrdu pre prijave.",
                user.getEmail());
    }

    @Transactional
    public RegisterPendingResponse registerHandyman(RegisterHandymanRequest req) {
        if (userRepository.existsByEmail(req.email()) || handymanRepository.existsByEmail(req.email())) {
            throw new IllegalArgumentException("Email je već registrovan");
        }
        String pib = PibUtils.normalizeOptional(req.pib());
        if (pib != null && handymanRepository.existsByPib(pib)) {
            throw new IllegalArgumentException("PIB je već registrovan");
        }
        String categoryIdsJson = handymanCategoryService.toJson(req.categoryIds());
        String phoneNormalized = PhoneUtils.normalizeOptional(req.phone());
        phoneUniquenessService.assertPhoneAvailable(phoneNormalized, null, null, null);
        Handyman handyman = Handyman.builder()
                .fullName(req.fullName())
                .email(req.email().trim().toLowerCase())
                .passwordHash(passwordEncoder.encode(req.password()))
                .phone(phoneNormalized)
                .phoneNormalized(phoneNormalized)
                .city(req.city())
                .bio(req.bio())
                .pib(pib)
                .categoryIdsJson(categoryIdsJson)
                .emailVerified(false)
                .build();
        handymanRepository.save(handyman);
        emailVerificationService.sendVerificationEmail(handyman.getEmail(), EmailVerificationService.ROLE_HANDYMAN, null);
        return new RegisterPendingResponse(
                "Registracija uspešna. Proverite email i kliknite na link za potvrdu pre prijave.",
                handyman.getEmail());
    }

    public AuthResponse login(LoginRequest req) {
        return loginInternal(req.email().trim().toLowerCase(), req.password());
    }

    private AuthResponse loginInternal(String email, String password) {
        var auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(email, password));
        UserPrincipal principal = (UserPrincipal) auth.getPrincipal();

        assertEmailVerified(email, principal.getRole());

        return issueAuthResponse(principal);
    }

    @Transactional
    public AuthResponse refreshWithToken(String refreshTokenValue) {
        RefreshToken stored = refreshTokenRepository.findByTokenAndRevokedFalse(refreshTokenValue)
                .orElseThrow(() -> new IllegalArgumentException("Nevažeći refresh token"));
        if (stored.getExpiresAt().isBefore(Instant.now())) {
            stored.setRevoked(true);
            refreshTokenRepository.save(stored);
            throw new IllegalArgumentException("Refresh token je istekao — prijavite se ponovo");
        }

        UserDetails userDetails = userDetailsService.loadUserByUsername(stored.getEmail());
        if (!userDetails.isEnabled()) {
            throw new IllegalStateException("Nalog nije aktivan");
        }
        UserPrincipal principal = (UserPrincipal) userDetails;
        if (!principal.getRole().equals(stored.getRole()) || !principal.getId().equals(stored.getUserId())) {
            throw new IllegalArgumentException("Nevažeći refresh token");
        }

        assertEmailVerified(principal.getEmail(), principal.getRole());

        stored.setRevoked(true);
        refreshTokenRepository.save(stored);

        return issueAuthResponse(principal);
    }

    @Transactional
    public void logout(String refreshTokenValue) {
        if (refreshTokenValue == null || refreshTokenValue.isBlank()) {
            return;
        }
        refreshTokenRepository.findByTokenAndRevokedFalse(refreshTokenValue).ifPresent(token -> {
            token.setRevoked(true);
            refreshTokenRepository.save(token);
        });
    }

    private AuthResponse issueAuthResponse(UserPrincipal principal) {
        String email = principal.getEmail();
        Object userDto = switch (principal.getRole()) {
            case "ROLE_CLIENT" -> DtoMapper.toUserDto(userRepository.findByEmail(email).orElseThrow());
            case "ROLE_HANDYMAN" -> DtoMapper.toHandymanDto(handymanRepository.findByEmail(email).orElseThrow());
            case "ROLE_ADMIN" -> {
                Admin admin = adminRepository.findByEmail(email).orElseThrow();
                yield Map.of(
                        "id", principal.getId(),
                        "email", email,
                        "fullName", admin.getFullName(),
                        "primaryAdmin", primaryAdminAuthorization.isPrimaryAdmin(email));
            }
            default -> throw new IllegalArgumentException("Nepoznata uloga");
        };
        String accessToken = jwtService.generateToken(principal);
        String refreshToken = createRefreshToken(principal);
        return DtoMapper.buildAuthResponse(accessToken, refreshToken, principal, userDto);
    }

    private String createRefreshToken(UserPrincipal principal) {
        String token = UUID.randomUUID().toString().replace("-", "");
        RefreshToken refreshToken = RefreshToken.builder()
                .token(token)
                .email(principal.getEmail())
                .role(principal.getRole())
                .userId(principal.getId())
                .expiresAt(Instant.now().plusMillis(refreshExpirationMs))
                .build();
        refreshTokenRepository.save(refreshToken);
        return token;
    }

    private void assertEmailVerified(String email, String role) {
        if ("ROLE_CLIENT".equals(role)) {
            User user = userRepository.findByEmail(email).orElseThrow();
            if (!Boolean.TRUE.equals(user.getEmailVerified())) {
                throw new IllegalStateException(
                        "Email nije potvrđen. Proverite inbox i kliknite na link za verifikaciju.");
            }
        }
        if ("ROLE_HANDYMAN".equals(role)) {
            Handyman handyman = handymanRepository.findByEmail(email).orElseThrow();
            if (!Boolean.TRUE.equals(handyman.getEmailVerified())) {
                throw new IllegalStateException(
                        "Email nije potvrđen. Proverite inbox i kliknite na link za verifikaciju.");
            }
        }
    }

    public EmailVerificationResponse verifyEmail(String token) {
        return emailVerificationService.verify(token);
    }

    public ResendVerificationResponse resendVerification(ResendVerificationRequest req) {
        return emailVerificationService.resend(req.email().trim().toLowerCase());
    }

    @Transactional
    public void forgotPassword(ForgotPasswordRequest req) {
        String email = req.email().trim().toLowerCase();
        String role = resolveRole(email);
        if (role == null) {
            return;
        }
        String token = UUID.randomUUID().toString();
        PasswordResetToken resetToken = PasswordResetToken.builder()
                .email(email)
                .token(token)
                .role(role)
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();
        resetTokenRepository.save(resetToken);
        String link = frontendUrl + "/reset-password?token=" + token;
        try {
            emailService.send(email, "Reset lozinke — Majstor 365",
                    "Poštovani,\n\nPrimili smo zahtev za reset lozinke.\n\n"
                            + "Kliknite na link da postavite novu lozinku:\n" + link
                            + "\n\nLink važi 1 sat. Ako niste vi tražili reset, ignorišite ovaj email.\n\nMajstor 365");
        } catch (Exception e) {
            log.error("[RESET] Greška pri slanju na {}: {}", email, e.getMessage());
            throw new IllegalStateException("Email nije poslat. Pokušajte ponovo za nekoliko minuta.");
        }
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest req) {
        PasswordResetToken resetToken = resetTokenRepository.findByTokenAndUsedFalse(req.token())
                .orElseThrow(() -> new IllegalArgumentException("Nevažeći ili istekao token"));
        if (resetToken.getExpiresAt().isBefore(Instant.now())) {
            throw new IllegalArgumentException("Token je istekao");
        }
        String hash = passwordEncoder.encode(req.password());
        switch (resetToken.getRole()) {
            case "ROLE_CLIENT" -> userRepository.findByEmail(resetToken.getEmail()).ifPresent(u -> {
                u.setPasswordHash(hash);
                userRepository.save(u);
            });
            case "ROLE_HANDYMAN" -> handymanRepository.findByEmail(resetToken.getEmail()).ifPresent(h -> {
                h.setPasswordHash(hash);
                handymanRepository.save(h);
            });
            case "ROLE_ADMIN" -> adminRepository.findByEmail(resetToken.getEmail()).ifPresent(a -> {
                a.setPasswordHash(hash);
                adminRepository.save(a);
            });
            default -> throw new IllegalArgumentException("Nepoznata uloga");
        }
        resetToken.setUsed(true);
        resetTokenRepository.save(resetToken);
    }

    private String resolveRole(String email) {
        if (userRepository.findByEmail(email).isPresent()) return "ROLE_CLIENT";
        if (handymanRepository.findByEmail(email).isPresent()) return "ROLE_HANDYMAN";
        if (adminRepository.findByEmail(email).isPresent()) return "ROLE_ADMIN";
        return null;
    }
}
