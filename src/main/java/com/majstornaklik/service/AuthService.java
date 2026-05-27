package com.majstornaklik.service;

import com.majstornaklik.dto.*;
import com.majstornaklik.entity.*;
import com.majstornaklik.repository.*;
import com.majstornaklik.security.JwtService;
import com.majstornaklik.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final HandymanRepository handymanRepository;
    private final AdminRepository adminRepository;
    private final PasswordResetTokenRepository resetTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final EmailService emailService;

    @Value("${app.frontend.url:http://localhost:3000}")
    private String frontendUrl;

    @Transactional
    public AuthResponse registerClient(RegisterClientRequest req) {
        if (userRepository.existsByEmail(req.email()) || handymanRepository.existsByEmail(req.email())) {
            throw new IllegalArgumentException("Email je već registrovan");
        }
        User user = User.builder()
                .fullName(req.fullName())
                .email(req.email())
                .passwordHash(passwordEncoder.encode(req.password()))
                .phone(req.phone())
                .city(req.city())
                .build();
        userRepository.save(user);
        emailService.send(user.getEmail(), "Dobrodošli na Majstor na klik",
                "Uspešno ste registrovani kao klijent.");
        return loginInternal(user.getEmail(), req.password());
    }

    @Transactional
    public AuthResponse registerHandyman(RegisterHandymanRequest req) {
        if (userRepository.existsByEmail(req.email()) || handymanRepository.existsByEmail(req.email())) {
            throw new IllegalArgumentException("Email je već registrovan");
        }
        Handyman handyman = Handyman.builder()
                .fullName(req.fullName())
                .email(req.email())
                .passwordHash(passwordEncoder.encode(req.password()))
                .phone(req.phone())
                .city(req.city())
                .bio(req.bio())
                .build();
        handymanRepository.save(handyman);
        emailService.send(handyman.getEmail(), "Dobrodošli na Majstor na klik",
                "Uspešno ste registrovani kao majstor.");
        return loginInternal(handyman.getEmail(), req.password());
    }

    public AuthResponse login(LoginRequest req) {
        return loginInternal(req.email(), req.password());
    }

    private AuthResponse loginInternal(String email, String password) {
        var auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(email, password));
        UserPrincipal principal = (UserPrincipal) auth.getPrincipal();

        String token = jwtService.generateToken(principal);
        Object userDto = switch (principal.getRole()) {
            case "ROLE_CLIENT" -> DtoMapper.toUserDto(userRepository.findByEmail(email).orElseThrow());
            case "ROLE_HANDYMAN" -> DtoMapper.toHandymanDto(handymanRepository.findByEmail(email).orElseThrow());
            case "ROLE_ADMIN" -> Map.of("id", principal.getId(), "email", email,
                    "fullName", adminRepository.findByEmail(email).map(Admin::getFullName).orElse("Admin"));
            default -> throw new IllegalArgumentException("Nepoznata uloga");
        };
        return DtoMapper.buildAuthResponse(token, principal, userDto);
    }

    public AuthResponse refresh(UserPrincipal principal) {
        String token = jwtService.generateToken(principal);
        String email = principal.getEmail();
        Object userDto = switch (principal.getRole()) {
            case "ROLE_CLIENT" -> DtoMapper.toUserDto(userRepository.findByEmail(email).orElseThrow());
            case "ROLE_HANDYMAN" -> DtoMapper.toHandymanDto(handymanRepository.findByEmail(email).orElseThrow());
            case "ROLE_ADMIN" -> Map.of("id", principal.getId(), "email", email);
            default -> throw new IllegalArgumentException("Nepoznata uloga");
        };
        return DtoMapper.buildAuthResponse(token, principal, userDto);
    }

    @Transactional
    public void forgotPassword(ForgotPasswordRequest req) {
        String role = resolveRole(req.email());
        if (role == null) {
            return;
        }
        String token = UUID.randomUUID().toString();
        PasswordResetToken resetToken = PasswordResetToken.builder()
                .email(req.email())
                .token(token)
                .role(role)
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();
        resetTokenRepository.save(resetToken);
        emailService.send(req.email(), "Reset lozinke",
                "Kliknite na link: " + frontendUrl + "/reset-password?token=" + token);
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
