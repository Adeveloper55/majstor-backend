package com.majstornaklik.dto;

import java.util.List;

public record HandymanSearchResponse(
        String categorySlug,
        String categoryName,
        String city,
        long totalCount,
        Double averageRating,
        int totalReviews,
        List<HandymanListingDto> content
) {}
