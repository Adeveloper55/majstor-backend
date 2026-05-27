package com.majstornaklik.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ContactMessageRequest(
        @NotBlank @Size(max = 100) String fullName,
        @NotBlank @Email String email,
        String phone,
        @NotBlank @Size(min = 10, max = 2000) String message,
        Boolean isContractor
) {}
