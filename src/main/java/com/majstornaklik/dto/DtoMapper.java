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
        return new HandymanPublicDto(h.getId(), h.getFullName(), h.getCity(), h.getBio(),
                h.getProfileImageUrl(), h.getIsVerified(), h.getAverageRating(), h.getTotalReviews());
    }

    public static CategoryDto toCategoryDto(Category c) {
        return new CategoryDto(c.getId(), c.getName(), c.getSlug(), c.getDescription(), c.getIconUrl(), c.getBaseTokenCost());
    }

    public static JobListingDto toJobDto(JobListing j, Double distance) {
        return toJobDto(j, distance, null);
    }

    public static JobListingDto toJobDto(JobListing j, Double distance, ClientContactDto clientContact) {
        return new JobListingDto(
                j.getId(), j.getUserId(), j.getCategory().getId(), toCategoryDto(j.getCategory()),
                j.getTitle(), j.getDescription(), j.getAddress(), j.getCity(),
                j.getLatitude(), j.getLongitude(), j.getImages(), j.getAiScore(), j.getTokenCost(),
                j.getStatus(), j.getSelectedHandymanId(), j.getCreatedAt(), distance, clientContact);
    }

    public static ClientContactDto toClientContact(User u) {
        return new ClientContactDto(u.getFullName(), u.getEmail(), u.getPhone(), u.getAddress(), u.getCity());
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

    public record HandymanPublicDto(UUID id, String fullName, String city, String bio,
                                   String profileImageUrl, Boolean isVerified,
                                   Double averageRating, Integer totalReviews) {}

    public record CategoryDto(Integer id, String name, String slug, String description,
                              String iconUrl, Integer baseTokenCost) {}

    public record JobListingDto(UUID id, UUID userId, Integer categoryId, CategoryDto category,
                                String title, String description, String address, String city,
                                Double latitude, Double longitude, String[] images,
                                Integer aiScore, Integer tokenCost, String status,
                                UUID selectedHandymanId, Instant createdAt, Double distance,
                                ClientContactDto clientContact) {}

    public static AuthResponse buildAuthResponse(String token, UserPrincipal principal, Object userDto) {
        return new AuthResponse(token, principal.getRole(), userDto);
    }
}
