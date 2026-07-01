package com.majstornaklik.dto;

import com.majstornaklik.entity.*;
import com.majstornaklik.security.UserPrincipal;

import com.majstornaklik.util.JsonUtils;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class DtoMapper {

    private DtoMapper() {}

    public static UserDto toUserDto(User u) {
        return new UserDto(u.getId(), u.getFullName(), u.getEmail(), u.getPhone(), u.getCity(),
                u.getLatitude(), u.getLongitude(), u.getProfileImageUrl(),
                u.getAverageRating(), u.getTotalReviews(), u.getCreatedAt());
    }

    public static HandymanDto toHandymanDto(Handyman h) {
        return new HandymanDto(
                h.getId(), h.getFullName(), h.getEmail(), h.getPhone(), h.getCity(),
                h.getLatitude(), h.getLongitude(), h.getBio(), h.getProfileImageUrl(),
                h.getIsVerified(), h.getTokenBalance(), h.getAverageRating(), h.getTotalReviews(),
                h.getCompanyName(), h.getPib(), h.getAddress(), h.getPostalCode(), h.getCountry(),
                h.getContactPerson(), h.getIsCompany(),
                JsonUtils.parseIntegerList(h.getCategoryIdsJson()), h.getCreatedAt());
    }

    public static HandymanPublicDto toHandymanPublicDto(Handyman h) {
        return toHandymanPublicDto(h, false);
    }

    public static HandymanPublicDto toHandymanPublicDto(Handyman h, boolean showContact) {
        return new HandymanPublicDto(h.getId(), h.getFullName(), h.getCompanyName(), h.getCity(), h.getBio(),
                h.getProfileImageUrl(), h.getIsVerified(), h.getAverageRating(), h.getTotalReviews(),
                showContact ? h.getPhone() : null, showContact ? h.getEmail() : null);
    }

    public static CategoryDto toCategoryDto(Category c) {
        return new CategoryDto(c.getId(), c.getName(), c.getSlug(), c.getDescription(), c.getIconUrl(), c.getBaseTokenCost());
    }

    public static JobListingDto toJobDto(JobListing j, Double distance) {
        return toJobDto(j, distance, null, null, false, false);
    }

    public static JobListingDto toJobDto(JobListing j, Double distance, ClientContactDto clientContact) {
        return toJobDto(j, distance, clientContact, null, false, false);
    }

    public static JobListingDto toJobDto(JobListing j, Double distance, ClientContactDto clientContact,
                                         boolean hideTokenCost) {
        return toJobDto(j, distance, clientContact, null, hideTokenCost, false);
    }

    public static JobListingDto toJobDto(JobListing j, Double distance, ClientContactDto clientContact,
                                         HandymanContactDto assignedHandymanContact,
                                         boolean hideTokenCost) {
        return toJobDto(j, distance, clientContact, assignedHandymanContact, hideTokenCost, false);
    }

    public static JobListingDto toJobDto(JobListing j, Double distance, ClientContactDto clientContact,
                                         HandymanContactDto assignedHandymanContact,
                                         boolean hideTokenCost, boolean hideExactLocation) {
        return toJobDto(j, distance, clientContact, assignedHandymanContact, hideTokenCost, hideExactLocation, false);
    }

    public static JobListingDto toJobDto(JobListing j, Double distance, ClientContactDto clientContact,
                                         HandymanContactDto assignedHandymanContact,
                                         boolean hideTokenCost, boolean hideExactLocation,
                                         boolean unlockedByMe) {
        Integer tokenCost = hideTokenCost ? null : j.getTokenCost();
        return new JobListingDto(
                j.getId(), j.getUserId(), j.getCategory().getId(), toCategoryDto(j.getCategory()),
                j.getTitle(), j.getDescription(),
                hideExactLocation ? null : j.getAddress(),
                j.getCity(),
                hideExactLocation ? null : j.getLatitude(),
                hideExactLocation ? null : j.getLongitude(),
                hideExactLocation ? false : Boolean.TRUE.equals(j.getLocationPinned()),
                j.getImages(), j.getAiScore(), tokenCost,
                j.getStatus(), j.getSelectedHandymanId(), j.getCreatedAt(), distance, clientContact,
                assignedHandymanContact, unlockedByMe);
    }

    public static ClientContactDto toClientContact(User u) {
        return new ClientContactDto(u.getFullName(), u.getEmail(), u.getPhone(), u.getAddress(), u.getCity());
    }

    public static HandymanContactDto toHandymanContact(Handyman h) {
        return new HandymanContactDto(h.getFullName(), h.getEmail(), h.getPhone());
    }

    public record UserDto(UUID id, String fullName, String email, String phone, String city,
                          Double latitude, Double longitude, String profileImageUrl,
                          Double averageRating, Integer totalReviews, Instant createdAt) {}

    public record HandymanDto(UUID id, String fullName, String email, String phone, String city,
                              Double latitude, Double longitude, String bio, String profileImageUrl,
                              Boolean isVerified, Integer tokenBalance, Double averageRating,
                              Integer totalReviews, String companyName, String pib, String address,
                              String postalCode, String country, String contactPerson, Boolean isCompany,
                              List<Integer> categoryIds, Instant createdAt) {}

    public record HandymanPublicDto(UUID id, String fullName, String companyName, String city, String bio,
                                   String profileImageUrl, Boolean isVerified,
                                   Double averageRating, Integer totalReviews,
                                   String phone, String email) {}

    public record CategoryDto(Integer id, String name, String slug, String description,
                              String iconUrl, Integer baseTokenCost) {}

    public record JobListingDto(UUID id, UUID userId, Integer categoryId, CategoryDto category,
                                String title, String description, String address, String city,
                                Double latitude, Double longitude, Boolean locationPinned, String[] images,
                                Integer aiScore, Integer tokenCost, String status,
                                UUID selectedHandymanId, Instant createdAt, Double distance,
                                ClientContactDto clientContact,
                                HandymanContactDto assignedHandymanContact,
                                Boolean unlockedByMe) {}

    public static AuthResponse buildAuthResponse(String token, String refreshToken, UserPrincipal principal, Object userDto) {
        return new AuthResponse(token, refreshToken, principal.getRole(), userDto);
    }
}
