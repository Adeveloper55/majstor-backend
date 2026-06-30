package com.majstornaklik.controller;

import com.majstornaklik.dto.*;
import com.majstornaklik.service.AuthService;
import com.majstornaklik.service.CompanyRegistrationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final CompanyRegistrationService companyRegistrationService;

    @PostMapping("/register/client")
    public RegisterPendingResponse registerClient(@Valid @RequestBody RegisterClientRequest req) {
        return authService.registerClient(req);
    }

    @PostMapping("/register/handyman")
    public RegisterPendingResponse registerHandyman(@Valid @RequestBody RegisterHandymanRequest req) {
        return authService.registerHandyman(req);
    }

    @PostMapping("/register/company")
    public CompanyRegistrationSubmitResponse registerCompany(@Valid @RequestBody RegisterCompanyRequest req) {
        return companyRegistrationService.submit(req);
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest req) {
        return authService.login(req);
    }

    @PostMapping("/refresh")
    public AuthResponse refresh(@Valid @RequestBody RefreshTokenRequest req) {
        return authService.refreshWithToken(req.refreshToken());
    }

    @PostMapping("/logout")
    public void logout(@RequestBody(required = false) LogoutRequest req) {
        if (req != null) {
            authService.logout(req.refreshToken());
        }
    }

    @PostMapping("/forgot-password")
    public void forgotPassword(@Valid @RequestBody ForgotPasswordRequest req) {
        authService.forgotPassword(req);
    }

    @PostMapping("/reset-password")
    public void resetPassword(@Valid @RequestBody ResetPasswordRequest req) {
        authService.resetPassword(req);
    }

    @GetMapping("/verify-email")
    public EmailVerificationResponse verifyEmail(@RequestParam String token) {
        return authService.verifyEmail(token);
    }

    @PostMapping("/resend-verification")
    public ResendVerificationResponse resendVerification(@Valid @RequestBody ResendVerificationRequest req) {
        return authService.resendVerification(req);
    }
}
