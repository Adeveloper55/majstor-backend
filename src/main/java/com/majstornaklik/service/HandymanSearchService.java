package com.majstornaklik.service;

import com.majstornaklik.dto.HandymanListingDto;
import com.majstornaklik.dto.HandymanProfileDto;
import com.majstornaklik.dto.HandymanSearchResponse;
import com.majstornaklik.entity.Category;
import com.majstornaklik.entity.Handyman;
import com.majstornaklik.entity.Review;
import com.majstornaklik.entity.User;
import com.majstornaklik.repository.CategoryRepository;
import com.majstornaklik.repository.HandymanRepository;
import com.majstornaklik.repository.ReviewRepository;
import com.majstornaklik.repository.UserRepository;
import com.majstornaklik.security.SecurityUtils;
import com.majstornaklik.util.CityUtils;
import com.majstornaklik.util.PhoneUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class HandymanSearchService {

    private final HandymanRepository handymanRepository;
    private final CategoryRepository categoryRepository;
    private final HandymanCategoryService handymanCategoryService;
    private final ReviewRepository reviewRepository;
    private final UserRepository userRepository;
    private final SecurityUtils securityUtils;

    @Transactional(readOnly = true)
    public HandymanSearchResponse search(String categorySlug, String city) {
        Category category = categoryRepository.findBySlug(categorySlug.trim())
                .orElseThrow(() -> new IllegalArgumentException("Kategorija nije pronađena"));

        boolean showContact = canShowContact();
        List<Handyman> matched = handymanRepository.findByIsActiveTrue().stream()
                .filter(h -> Boolean.TRUE.equals(h.getEmailVerified()))
                .filter(h -> handymanCategoryService.getCategoryIds(h).contains(category.getId()))
                .filter(h -> city == null || city.isBlank() || CityUtils.matchesCity(h.getCity(), city))
                .sorted(Comparator
                        .comparing(Handyman::getAverageRating, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(Handyman::getTotalReviews, Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();

        List<HandymanListingDto> listings = matched.stream()
                .map(h -> toListingDto(h, showContact))
                .toList();

        double avgRating = listings.stream()
                .map(HandymanListingDto::averageRating)
                .filter(r -> r != null && r > 0)
                .mapToDouble(Double::doubleValue)
                .average()
                .orElse(0);

        int totalReviews = listings.stream()
                .mapToInt(h -> h.totalReviews() != null ? h.totalReviews() : 0)
                .sum();

        return new HandymanSearchResponse(
                category.getSlug(),
                category.getName(),
                city != null ? city.trim() : "",
                listings.size(),
                listings.isEmpty() ? null : Math.round(avgRating * 10.0) / 10.0,
                totalReviews,
                listings);
    }

    @Transactional(readOnly = true)
    public long countByCategorySlug(String categorySlug) {
        Category category = categoryRepository.findBySlug(categorySlug.trim())
                .orElseThrow(() -> new IllegalArgumentException("Kategorija nije pronađena"));
        return handymanRepository.findByIsActiveTrue().stream()
                .filter(h -> Boolean.TRUE.equals(h.getEmailVerified()))
                .filter(h -> handymanCategoryService.getCategoryIds(h).contains(category.getId()))
                .count();
    }

    @Transactional(readOnly = true)
    public HandymanProfileDto getPublicProfile(UUID id) {
        Handyman h = handymanRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Izvođač nije pronađen"));
        if (!Boolean.TRUE.equals(h.getIsActive()) || !Boolean.TRUE.equals(h.getEmailVerified())) {
            throw new IllegalArgumentException("Izvođač nije dostupan");
        }

        boolean showContact = canShowContactForCurrentUser();
        List<Integer> categoryIds = handymanCategoryService.getCategoryIds(h);
        List<String> serviceNames = categoryRepository.findAllById(categoryIds).stream()
                .map(Category::getName)
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .toList();

        HandymanListingDto.ReviewSnippetDto latestReview = findLatestReview(h.getId());
        Integer years = h.getCreatedAt() != null
                ? (int) ChronoUnit.YEARS.between(h.getCreatedAt(), Instant.now())
                : null;

        return new HandymanProfileDto(
                h.getId(),
                h.getFullName(),
                h.getCompanyName(),
                resolveDisplayName(h),
                h.getContactPerson(),
                h.getCity(),
                h.getBio(),
                h.getProfileImageUrl(),
                h.getIsVerified(),
                h.getAverageRating(),
                h.getTotalReviews(),
                showContact ? h.getPhone() : null,
                PhoneUtils.maskForDisplay(h.getPhone()),
                showContact ? h.getEmail() : null,
                showContact,
                serviceNames,
                latestReview,
                years != null && years > 0 ? years : null,
                h.getCreatedAt());
    }

    private HandymanListingDto toListingDto(Handyman h, boolean showContact) {
        String displayName = resolveDisplayName(h);
        HandymanListingDto.ReviewSnippetDto latestReview = findLatestReview(h.getId());
        Integer years = h.getCreatedAt() != null
                ? (int) ChronoUnit.YEARS.between(h.getCreatedAt(), Instant.now())
                : null;

        return new HandymanListingDto(
                h.getId(),
                h.getFullName(),
                h.getCompanyName(),
                displayName,
                h.getCity(),
                h.getBio(),
                h.getProfileImageUrl(),
                h.getIsVerified(),
                h.getAverageRating(),
                h.getTotalReviews(),
                showContact ? h.getPhone() : null,
                PhoneUtils.maskForDisplay(h.getPhone()),
                showContact ? h.getEmail() : null,
                latestReview,
                years != null && years > 0 ? years : null,
                h.getCreatedAt());
    }

    private HandymanListingDto.ReviewSnippetDto findLatestReview(java.util.UUID handymanId) {
        return reviewRepository.findByRevieweeHandymanId(handymanId).stream()
                .max(Comparator.comparing(Review::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder())))
                .map(review -> new HandymanListingDto.ReviewSnippetDto(
                        review.getRating(),
                        review.getComment(),
                        resolveReviewerName(review)))
                .orElse(null);
    }

    private String resolveReviewerName(Review review) {
        if (review.getReviewerUserId() != null) {
            return userRepository.findById(review.getReviewerUserId())
                    .map(this::maskName)
                    .orElse("Korisnik");
        }
        return "Korisnik";
    }

    private String maskName(User user) {
        String name = user.getFullName();
        if (name == null || name.isBlank()) {
            return "Korisnik";
        }
        String[] parts = name.trim().split("\\s+");
        if (parts.length == 1) {
            return parts[0].charAt(0) + ".";
        }
        return parts[0] + " " + parts[parts.length - 1].charAt(0) + ".";
    }

    private String resolveDisplayName(Handyman h) {
        if (h.getCompanyName() != null && !h.getCompanyName().isBlank()) {
            return h.getCompanyName().trim();
        }
        return h.getFullName();
    }

    private boolean canShowContact() {
        return canShowContactForCurrentUser();
    }

    public boolean canShowContactForCurrentUser() {
        var principal = securityUtils.getCurrentUserOrNull();
        if (principal == null) {
            return false;
        }
        String role = principal.getRole();
        return "ROLE_CLIENT".equals(role) || "ROLE_HANDYMAN".equals(role);
    }
}
