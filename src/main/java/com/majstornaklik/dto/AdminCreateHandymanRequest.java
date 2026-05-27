package com.majstornaklik.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AdminCreateHandymanRequest(
        @NotBlank @Size(max = 100) String fullName,
        @NotBlank @Email String email,
        @NotBlank @Size(min = 6) String password,
        String phone,
        String city,
        String bio,
        @Min(0) Integer initialTokens
) {}
