package com.majstornaklik.dto;

public record ScorePreviewResponse(
        int score,
        String reason,
        int tokenCost
) {}
