package com.majstornaklik.dto;

import jakarta.validation.constraints.*;

import java.util.List;

public record RegisterCompanyRequest(
        @NotBlank @Email String email,
        @NotBlank String phone,
        @NotBlank String normalizedPhone,
        @NotEmpty List<String> selectedServiceIds,
        @NotEmpty List<String> selectedServiceNames,
        @Size(max = 100) String companyShortDescription,
        @NotEmpty List<String> selectedDistricts,
        @NotBlank @Size(max = 200) String companyName,
        @NotBlank @Pattern(regexp = "\\d{9}", message = "PIB mora imati 9 cifara") String pib,
        @NotBlank String address,
        @NotBlank String postalCode,
        @NotBlank String city,
        @NotBlank String country,
        @NotBlank @Size(max = 100) String contactPerson,
        @NotBlank @Size(min = 6) String password,
        @AssertTrue(message = "Morate prihvatiti uslove korišćenja") boolean acceptTerms
) {}
