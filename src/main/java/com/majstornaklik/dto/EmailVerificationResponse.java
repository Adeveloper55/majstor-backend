package com.majstornaklik.dto;

public record EmailVerificationResponse(
        String status,
        String message,
        String accountType
) {
    public static EmailVerificationResponse of(String status, String message) {
        return new EmailVerificationResponse(status, message, null);
    }

    public static EmailVerificationResponse of(String status, String message, String accountType) {
        return new EmailVerificationResponse(status, message, accountType);
    }
}
