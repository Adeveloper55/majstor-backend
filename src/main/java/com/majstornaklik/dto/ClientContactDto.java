package com.majstornaklik.dto;

public record ClientContactDto(
        String fullName,
        String email,
        String phone,
        String address,
        String city
) {}
