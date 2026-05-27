package com.majstornaklik.dto;

import jakarta.validation.constraints.NotNull;

public record AdjustTokensRequest(
        @NotNull Integer amount,
        String description
) {}
