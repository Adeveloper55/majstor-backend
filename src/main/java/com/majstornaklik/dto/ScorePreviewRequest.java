package com.majstornaklik.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ScorePreviewRequest(
        @NotBlank String description,
        @NotNull Integer categoryId
) {}
