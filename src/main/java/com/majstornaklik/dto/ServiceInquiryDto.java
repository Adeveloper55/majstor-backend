package com.majstornaklik.dto;

import com.majstornaklik.entity.ServiceInquiry;

import java.time.Instant;
import java.util.UUID;

public record ServiceInquiryDto(
        UUID id,
        String categorySlug,
        String categoryName,
        String city,
        String startTimeline,
        String shortDescription,
        String detailedDescription,
        String salutation,
        String fullName,
        String email,
        String phone,
        String status,
        Instant createdAt
) {
    public static ServiceInquiryDto from(ServiceInquiry inquiry) {
        return new ServiceInquiryDto(
                inquiry.getId(),
                inquiry.getCategorySlug(),
                inquiry.getCategoryName(),
                inquiry.getCity(),
                inquiry.getStartTimeline(),
                inquiry.getShortDescription(),
                inquiry.getDetailedDescription(),
                inquiry.getSalutation(),
                inquiry.getFullName(),
                inquiry.getEmail(),
                inquiry.getPhone(),
                inquiry.getStatus(),
                inquiry.getCreatedAt()
        );
    }
}
