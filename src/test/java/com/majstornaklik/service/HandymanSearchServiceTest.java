package com.majstornaklik.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.majstornaklik.dto.HandymanListingDto;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HandymanSearchServiceTest {

    @Mock
    private HandymanRepository handymanRepository;
    @Mock
    private CategoryRepository categoryRepository;
    @Mock
    private HandymanCategoryService handymanCategoryService;
    @Mock
    private ReviewRepository reviewRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private SecurityUtils securityUtils;

    @InjectMocks
    private HandymanSearchService handymanSearchService;

    private Category cleaningCategory;
    private Handyman handyman;
    private UUID handymanId;

    @BeforeEach
    void setUp() {
        handymanId = UUID.randomUUID();
        cleaningCategory = Category.builder().id(11).name("Agencija za čišćenje").slug("agencija-za-ciscenje").build();
        handyman = Handyman.builder()
                .id(handymanId)
                .fullName("TestLokacija")
                .companyName("TestLokacija")
                .email("test@example.com")
                .passwordHash("hash")
                .phone("+381635964912")
                .city("Niš")
                .isActive(true)
                .emailVerified(true)
                .isVerified(true)
                .averageRating(0.0)
                .totalReviews(0)
                .createdAt(Instant.parse("2024-01-01T00:00:00Z"))
                .build();
    }

    @Test
    void searchReturnsListingsForMatchingHandyman() {
        when(categoryRepository.findBySlug("agencija-za-ciscenje")).thenReturn(Optional.of(cleaningCategory));
        when(handymanRepository.findByIsActiveTrue()).thenReturn(List.of(handyman));
        when(handymanCategoryService.getCategoryIds(handyman)).thenReturn(List.of(11));
        when(securityUtils.getCurrentUserOrNull()).thenReturn(null);
        when(reviewRepository.findByRevieweeHandymanId(handymanId)).thenReturn(List.of());

        HandymanSearchResponse response = handymanSearchService.search("agencija-za-ciscenje", "Niš");

        assertThat(response.totalCount()).isEqualTo(1);
        assertThat(response.content()).hasSize(1);
        assertThat(response.content().get(0).displayName()).isEqualTo("TestLokacija");
    }

    @Test
    void searchIncludesHandymanWithReviewAndMaskedReviewer() throws Exception {
        UUID reviewerId = UUID.randomUUID();
        Review review = Review.builder()
                .id(UUID.randomUUID())
                .jobListingId(UUID.randomUUID())
                .reviewerType("CLIENT")
                .reviewerUserId(reviewerId)
                .revieweeHandymanId(handymanId)
                .rating(5)
                .comment("Odličan posao")
                .createdAt(Instant.now())
                .build();
        User reviewer = User.builder()
                .id(reviewerId)
                .fullName("M")
                .email("client@example.com")
                .passwordHash("hash")
                .build();

        when(categoryRepository.findBySlug("agencija-za-ciscenje")).thenReturn(Optional.of(cleaningCategory));
        when(handymanRepository.findByIsActiveTrue()).thenReturn(List.of(handyman));
        when(handymanCategoryService.getCategoryIds(handyman)).thenReturn(List.of(11));
        when(securityUtils.getCurrentUserOrNull()).thenReturn(null);
        when(reviewRepository.findByRevieweeHandymanId(handymanId)).thenReturn(List.of(review));
        when(userRepository.findById(reviewerId)).thenReturn(Optional.of(reviewer));

        HandymanSearchResponse response = handymanSearchService.search("agencija-za-ciscenje", "Niš");

        ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());
        String json = mapper.writeValueAsString(response);
        assertThat(json).contains("TestLokacija");
        assertThat(response.content().get(0).latestReview()).isNotNull();
        assertThat(response.content().get(0).latestReview().reviewerName()).isEqualTo("M.");
    }

    @Test
    void searchMatchesCoverageDistrictWhenCityDiffers() {
        handyman.setCity("Beograd");
        handyman.setCoverageDistrictsJson("[\"Grad Niš\"]");

        when(categoryRepository.findBySlug("agencija-za-ciscenje")).thenReturn(Optional.of(cleaningCategory));
        when(handymanRepository.findByIsActiveTrue()).thenReturn(List.of(handyman));
        when(handymanCategoryService.getCategoryIds(handyman)).thenReturn(List.of(11));
        when(securityUtils.getCurrentUserOrNull()).thenReturn(null);

        HandymanSearchResponse response = handymanSearchService.search("agencija-za-ciscenje", "Niš");

        assertThat(response.totalCount()).isEqualTo(1);
    }

    @Test
    void searchIncludesVerifiedHandymanWithoutEmailVerified() {
        handyman.setEmailVerified(false);
        handyman.setIsVerified(true);

        when(categoryRepository.findBySlug("agencija-za-ciscenje")).thenReturn(Optional.of(cleaningCategory));
        when(handymanRepository.findByIsActiveTrue()).thenReturn(List.of(handyman));
        when(handymanCategoryService.getCategoryIds(handyman)).thenReturn(List.of(11));
        when(securityUtils.getCurrentUserOrNull()).thenReturn(null);
        when(reviewRepository.findByRevieweeHandymanId(any())).thenReturn(List.of());

        HandymanSearchResponse response = handymanSearchService.search("agencija-za-ciscenje", "Niš");

        assertThat(response.totalCount()).isEqualTo(1);
    }
}
