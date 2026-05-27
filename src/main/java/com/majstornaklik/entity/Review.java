package com.majstornaklik.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "reviews")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Review {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "job_listing_id", nullable = false)
    private UUID jobListingId;

    @Column(name = "reviewer_type", nullable = false, length = 20)
    private String reviewerType;

    @Column(name = "reviewer_user_id")
    private UUID reviewerUserId;

    @Column(name = "reviewer_handyman_id")
    private UUID reviewerHandymanId;

    @Column(name = "reviewee_user_id")
    private UUID revieweeUserId;

    @Column(name = "reviewee_handyman_id")
    private UUID revieweeHandymanId;

    @Column(nullable = false)
    private Integer rating;

    @Column(columnDefinition = "TEXT")
    private String comment;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;
}
