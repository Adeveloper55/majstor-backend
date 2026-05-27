package com.majstornaklik.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateJobRequest(
        @NotNull Integer categoryId,
        @NotBlank String title,
        @NotBlank String description,
        String address,
        String city,
        Double latitude,
        Double longitude,
        String[] images
) {}
