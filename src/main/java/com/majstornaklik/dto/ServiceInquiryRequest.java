package com.majstornaklik.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ServiceInquiryRequest(
        @NotBlank String categorySlug,
        @NotBlank String categoryName,
        @NotBlank String city,
        @NotBlank String startTimeline,
        @Size(max = 120) String shortDescription,
        @NotBlank String detailedDescription,
        String salutation,
        @NotBlank String fullName,
        @NotBlank @Email String email,
        String phone
) {}
