package com.majstornaklik.dto;

public record AuthResponse(
        String token,
        String refreshToken,
        String role,
        Object user
) {}
