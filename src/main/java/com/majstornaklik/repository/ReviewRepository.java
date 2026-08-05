package com.majstornaklik.repository;

import com.majstornaklik.entity.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ReviewRepository extends JpaRepository<Review, UUID> {
    List<Review> findByRevieweeUserId(UUID userId);
    List<Review> findByRevieweeHandymanId(UUID handymanId);
    Page<Review> findByRevieweeUserId(UUID userId, Pageable pageable);
    Page<Review> findByRevieweeHandymanId(UUID handymanId, Pageable pageable);
    Optional<Review> findByJobListingIdAndReviewerType(UUID jobListingId, String reviewerType);
    List<Review> findByJobListingId(UUID jobListingId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM Review r WHERE r.jobListingId = :jobListingId")
    int deleteByJobListingId(@Param("jobListingId") UUID jobListingId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM Review r WHERE r.reviewerUserId = :userId OR r.revieweeUserId = :userId")
    int deleteAllForUser(@Param("userId") UUID userId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM Review r WHERE r.reviewerHandymanId = :handymanId OR r.revieweeHandymanId = :handymanId")
    int deleteAllForHandyman(@Param("handymanId") UUID handymanId);
}
