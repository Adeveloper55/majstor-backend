package com.majstornaklik.service;

import com.majstornaklik.dto.SubmitReviewRequest;
import com.majstornaklik.entity.*;
import com.majstornaklik.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final JobListingRepository jobListingRepository;
    private final UserRepository userRepository;
    private final HandymanRepository handymanRepository;

    @Transactional
    public Map<String, Object> submitReview(UUID userId, String role, SubmitReviewRequest req) {
        JobListing job = jobListingRepository.findById(req.jobId())
                .orElseThrow(() -> new IllegalArgumentException("Posao nije pronađen"));
        if (!"COMPLETED".equals(job.getStatus())) {
            throw new IllegalArgumentException("Recenzija je moguća samo za završene poslove");
        }

        String reviewerType = "ROLE_CLIENT".equals(role) ? "CLIENT" : "HANDYMAN";
        if (reviewRepository.findByJobListingIdAndReviewerType(req.jobId(), reviewerType).isPresent()) {
            throw new IllegalArgumentException("Već ste ostavili recenziju");
        }

        Review.ReviewBuilder builder = Review.builder()
                .jobListingId(req.jobId())
                .reviewerType(reviewerType)
                .rating(req.rating())
                .comment(req.comment());

        if ("ROLE_CLIENT".equals(role)) {
            if (!job.getUserId().equals(userId)) {
                throw new IllegalStateException("Nemate pristup");
            }
            builder.reviewerUserId(userId);
            builder.revieweeHandymanId(job.getSelectedHandymanId());
        } else {
            if (!userId.equals(job.getSelectedHandymanId())) {
                throw new IllegalStateException("Nemate pristup");
            }
            builder.reviewerHandymanId(userId);
            builder.revieweeUserId(job.getUserId());
        }

        Review review = reviewRepository.save(builder.build());
        recalculateRatings(review);
        return Map.of("id", review.getId(), "rating", review.getRating());
    }

    public Map<String, Object> getReviewStatus(UUID jobId, UUID userId, String role) {
        JobListing job = jobListingRepository.findById(jobId)
                .orElseThrow(() -> new IllegalArgumentException("Posao nije pronađen"));

        boolean isParticipant = "ROLE_CLIENT".equals(role) && job.getUserId().equals(userId)
                || "ROLE_HANDYMAN".equals(role) && userId.equals(job.getSelectedHandymanId());

        String reviewerType = "ROLE_CLIENT".equals(role) ? "CLIENT" : "HANDYMAN";
        boolean alreadyReviewed = reviewRepository.findByJobListingIdAndReviewerType(jobId, reviewerType).isPresent();

        return Map.of(
                "jobStatus", job.getStatus(),
                "canReview", isParticipant && "COMPLETED".equals(job.getStatus()) && !alreadyReviewed,
                "alreadyReviewed", alreadyReviewed,
                "isParticipant", isParticipant
        );
    }

    public Page<Review> getReviewsForUser(UUID userId, Pageable pageable) {
        return reviewRepository.findByRevieweeUserId(userId, pageable);
    }

    public Page<Review> getReviewsForHandyman(UUID handymanId, Pageable pageable) {
        return reviewRepository.findByRevieweeHandymanId(handymanId, pageable);
    }

    @Transactional
    public void deleteReview(UUID id) {
        Review review = reviewRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Recenzija nije pronađena"));
        UUID revieweeUserId = review.getRevieweeUserId();
        UUID revieweeHandymanId = review.getRevieweeHandymanId();
        reviewRepository.delete(review);

        if (revieweeUserId != null) {
            updateUserRating(revieweeUserId, reviewRepository.findByRevieweeUserId(revieweeUserId));
        }
        if (revieweeHandymanId != null) {
            updateHandymanRating(revieweeHandymanId, reviewRepository.findByRevieweeHandymanId(revieweeHandymanId));
        }
    }

    private void recalculateRatings(Review review) {
        if (review.getRevieweeUserId() != null) {
            updateUserRating(review.getRevieweeUserId(), reviewRepository.findByRevieweeUserId(review.getRevieweeUserId()));
        }
        if (review.getRevieweeHandymanId() != null) {
            updateHandymanRating(review.getRevieweeHandymanId(), reviewRepository.findByRevieweeHandymanId(review.getRevieweeHandymanId()));
        }
    }

    private void updateUserRating(UUID userId, List<Review> reviews) {
        userRepository.findById(userId).ifPresent(u -> {
            double avg = reviews.stream().mapToInt(Review::getRating).average().orElse(0);
            u.setAverageRating(avg);
            u.setTotalReviews(reviews.size());
            userRepository.save(u);
        });
    }

    private void updateHandymanRating(UUID handymanId, List<Review> reviews) {
        handymanRepository.findById(handymanId).ifPresent(h -> {
            double avg = reviews.stream().mapToInt(Review::getRating).average().orElse(0);
            h.setAverageRating(avg);
            h.setTotalReviews(reviews.size());
            handymanRepository.save(h);
        });
    }
}
