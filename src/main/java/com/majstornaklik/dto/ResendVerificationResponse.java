package com.majstornaklik.dto;

public record ResendVerificationResponse(String message, boolean emailSent) {

    public static ResendVerificationResponse dispatched() {
        return new ResendVerificationResponse(
                "Novi link za verifikaciju je poslat. Proverite inbox i spam folder.", true);
    }

    public static ResendVerificationResponse notFound() {
        return new ResendVerificationResponse(
                "Nalog sa tim emailom nije pronađen ili je email već potvrđen. Registrujte se ponovo ili se prijavite.",
                false);
    }
}
