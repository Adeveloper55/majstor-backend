package com.majstornaklik.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record AdminCreateJobRequest(
        @NotNull UUID clientId,
        @NotNull Integer categoryId,
        @NotBlank String title,
        @NotBlank String description,
        String address,
        String city,
        Double latitude,
        Double longitude,
        String[] images,
        @NotNull @Min(1) Integer tokenCost
) {}
