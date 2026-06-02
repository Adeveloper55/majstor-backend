package com.majstornaklik.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

public record RegisterHandymanRequest(
        @NotBlank @Size(max = 100) String fullName,
        @NotBlank @Email String email,
        @NotBlank @Size(min = 6) String password,
        String phone,
        String city,
        String bio,
        @NotBlank @Pattern(regexp = "\\d{9}", message = "PIB mora imati 9 cifara") String pib,
        @NotEmpty @Size(min = 1, max = 10) List<Integer> categoryIds
) {}
