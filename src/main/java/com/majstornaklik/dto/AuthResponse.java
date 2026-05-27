package com.majstornaklik.dto;

public record AuthResponse(
        String token,
        String role,
        Object user
) {}
