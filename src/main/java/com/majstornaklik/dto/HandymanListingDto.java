package com.majstornaklik.dto;

import java.time.Instant;
import java.util.UUID;

public record HandymanListingDto(
        UUID id,
        String fullName,
        String companyName,
        String displayName,
        String city,
        String bio,
        String profileImageUrl,
        Boolean isVerified,
        Double averageRating,
        Integer totalReviews,
        String phone,
        String email,
        ReviewSnippetDto latestReview,
        Integer yearsExperience,
        Instant memberSince
) {
    public record ReviewSnippetDto(Integer rating, String comment, String reviewerName) {}
}
