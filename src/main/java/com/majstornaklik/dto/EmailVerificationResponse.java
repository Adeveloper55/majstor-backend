package com.majstornaklik.dto;

public record EmailVerificationResponse(
        String status,
        String message
) {
    public static EmailVerificationResponse of(String status, String message) {
        return new EmailVerificationResponse(status, message);
    }
}
