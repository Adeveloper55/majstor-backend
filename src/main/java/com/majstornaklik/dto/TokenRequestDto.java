package com.majstornaklik.dto;

import jakarta.validation.constraints.NotNull;

public record TokenRequestDto(
        @NotNull Integer packageId,
        String paymentReference
) {}
