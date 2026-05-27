package com.majstornaklik.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterHandymanRequest(
        @NotBlank @Size(max = 100) String fullName,
        @NotBlank @Email String email,
        @NotBlank @Size(min = 6) String password,
        String phone,
        String city,
        String bio
) {}
