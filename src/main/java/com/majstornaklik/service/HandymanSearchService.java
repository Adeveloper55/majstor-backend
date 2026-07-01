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
import com.majstornaklik.util.JsonUtils;
import com.majstornaklik.util.PhoneUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.Period;
import java.time.ZoneOffset;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class HandymanSearchService {

    private static final Map<String, Set<String>> RELATED_CATEGORY_SLUGS = Map.of(
            "agencija-za-ciscenje", Set.of("agencija-za-ciscenje", "ciscenje", "ciscenje-dimnjaka", "pranje-dvorista-fasade-krova"),
            "ciscenje", Set.of("agencija-za-ciscenje", "ciscenje", "ciscenje-dimnjaka", "pranje-dvorista-fasade-krova")
    );

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

        Set<Integer> matchingCategoryIds = resolveMatchingCategoryIds(category);
        boolean showContact = canShowContact();
        String cityFilter = city != null ? city.trim() : "";

        List<Handyman> matched = handymanRepository.findByIsActiveTrue().stream()
                .filter(this::isVisibleInSearch)
                .filter(h -> matchesCategory(h, matchingCategoryIds))
                .filter(h -> cityFilter.isBlank() || matchesCity(h, cityFilter))
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
                cityFilter,
                listings.size(),
                listings.isEmpty() ? null : Math.round(avgRating * 10.0) / 10.0,
                totalReviews,
                listings);
    }

    @Transactional(readOnly = true)
    public long countByCategorySlug(String categorySlug) {
        Category category = categoryRepository.findBySlug(categorySlug.trim())
                .orElseThrow(() -> new IllegalArgumentException("Kategorija nije pronađena"));
        Set<Integer> matchingCategoryIds = resolveMatchingCategoryIds(category);
        return handymanRepository.findByIsActiveTrue().stream()
                .filter(this::isVisibleInSearch)
                .filter(h -> matchesCategory(h, matchingCategoryIds))
                .count();
    }

    @Transactional(readOnly = true)
    public HandymanProfileDto getPublicProfile(UUID id) {
        Handyman h = handymanRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Izvođač nije pronađen"));
        if (!isVisibleInSearch(h)) {
            throw new IllegalArgumentException("Izvođač nije dostupan");
        }

        boolean showContact = canShowContactForCurrentUser();
        List<Integer> categoryIds = handymanCategoryService.getCategoryIds(h);
        List<String> serviceNames = categoryRepository.findAllById(categoryIds).stream()
                .map(Category::getName)
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .toList();

        HandymanListingDto.ReviewSnippetDto latestReview = findLatestReview(h.getId());
        Integer years = yearsOfExperience(h.getCreatedAt());

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
                years,
                h.getCreatedAt());
    }

    private Set<Integer> resolveMatchingCategoryIds(Category category) {
        Set<String> slugGroup = RELATED_CATEGORY_SLUGS.getOrDefault(category.getSlug(), Set.of(category.getSlug()));
        Set<Integer> ids = categoryRepository.findAll().stream()
                .filter(c -> slugGroup.contains(c.getSlug()))
                .map(Category::getId)
                .collect(Collectors.toCollection(HashSet::new));
        ids.add(category.getId());
        return ids;
    }

    private boolean isVisibleInSearch(Handyman h) {
        if (!Boolean.TRUE.equals(h.getIsActive())) {
            return false;
        }
        return Boolean.TRUE.equals(h.getEmailVerified()) || Boolean.TRUE.equals(h.getIsVerified());
    }

    private boolean matchesCategory(Handyman h, Set<Integer> matchingCategoryIds) {
        List<Integer> categoryIds = handymanCategoryService.getCategoryIds(h);
        if (categoryIds.stream().anyMatch(matchingCategoryIds::contains)) {
            return true;
        }
        List<String> serviceNames = JsonUtils.parseStringList(h.getServiceCategoriesJson());
        if (serviceNames.isEmpty()) {
            return false;
        }
        return handymanCategoryService.resolveCategoryIdsFromNames(serviceNames).stream()
                .anyMatch(matchingCategoryIds::contains);
    }

    private boolean matchesCity(Handyman h, String city) {
        if (CityUtils.matchesCity(h.getCity(), city)) {
            return true;
        }
        if (CityUtils.matchesCity(h.getAddress(), city)) {
            return true;
        }
        if (CityUtils.matchesCity(h.getPostalCode(), city)) {
            return true;
        }
        return JsonUtils.parseStringList(h.getCoverageDistrictsJson()).stream()
                .anyMatch(district -> CityUtils.matchesCity(district, city));
    }

    private HandymanListingDto toListingDto(Handyman h, boolean showContact) {
        String displayName = resolveDisplayName(h);
        HandymanListingDto.ReviewSnippetDto latestReview = findLatestReview(h.getId());
        Integer years = yearsOfExperience(h.getCreatedAt());

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
                years,
                h.getCreatedAt());
    }

    private Integer yearsOfExperience(Instant createdAt) {
        if (createdAt == null) {
            return null;
        }
        LocalDate joined = LocalDate.ofInstant(createdAt, ZoneOffset.UTC);
        int years = Period.between(joined, LocalDate.now(ZoneOffset.UTC)).getYears();
        return years > 0 ? years : null;
    }

    private HandymanListingDto.ReviewSnippetDto findLatestReview(UUID handymanId) {
        try {
            return reviewRepository.findByRevieweeHandymanId(handymanId).stream()
                    .max(Comparator.comparing(Review::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder())))
                    .map(review -> new HandymanListingDto.ReviewSnippetDto(
                            review.getRating(),
                            review.getComment(),
                            resolveReviewerName(review)))
                    .orElse(null);
        } catch (RuntimeException ex) {
            return null;
        }
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
            String part = parts[0];
            return part.isEmpty() ? "Korisnik" : part.substring(0, 1) + ".";
        }
        String first = parts[0];
        String last = parts[parts.length - 1];
        if (first.isEmpty() || last.isEmpty()) {
            return "Korisnik";
        }
        return first + " " + last.substring(0, 1) + ".";
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
