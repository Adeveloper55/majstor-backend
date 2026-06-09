package com.majstornaklik.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record AdminApproveJobRequest(
        @NotNull @Min(1) Integer tokenCost
) {}
