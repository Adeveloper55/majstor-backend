package com.majstornaklik.dto;

import com.majstornaklik.entity.CompanyRegistrationRequest;
import com.majstornaklik.util.JsonUtils;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record CompanyRegistrationDto(
        UUID id,
        String email,
        String phone,
        String normalizedPhone,
        List<String> selectedServiceIds,
        List<String> selectedServiceNames,
        String companyShortDescription,
        List<String> selectedDistricts,
        String companyName,
        String pib,
        String address,
        String postalCode,
        String city,
        String country,
        String contactPerson,
        String status,
        String adminNote,
        UUID handymanId,
        Instant createdAt,
        Instant reviewedAt
) {
    public static CompanyRegistrationDto from(CompanyRegistrationRequest r) {
        return new CompanyRegistrationDto(
                r.getId(),
                r.getEmail(),
                r.getPhone(),
                r.getNormalizedPhone(),
                JsonUtils.parseStringList(r.getSelectedServiceIds()),
                JsonUtils.parseStringList(r.getSelectedServiceNames()),
                r.getCompanyShortDescription(),
                JsonUtils.parseStringList(r.getSelectedDistricts()),
                r.getCompanyName(),
                r.getPib(),
                r.getAddress(),
                r.getPostalCode(),
                r.getCity(),
                r.getCountry(),
                r.getContactPerson(),
                r.getStatus(),
                r.getAdminNote(),
                r.getHandymanId(),
                r.getCreatedAt(),
                r.getReviewedAt()
        );
    }
}
