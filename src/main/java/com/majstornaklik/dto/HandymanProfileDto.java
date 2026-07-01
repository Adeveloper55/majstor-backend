package com.majstornaklik.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record HandymanProfileDto(
        UUID id,
        String fullName,
        String companyName,
        String displayName,
        String contactPerson,
        String city,
        String bio,
        String profileImageUrl,
        Boolean isVerified,
        Double averageRating,
        Integer totalReviews,
        String phone,
        String maskedPhone,
        String email,
        boolean contactVisible,
        List<String> serviceNames,
        HandymanListingDto.ReviewSnippetDto latestReview,
        Integer yearsExperience,
        Instant memberSince
) {}
