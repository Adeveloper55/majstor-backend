package com.majstornaklik.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "job_listings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JobListing {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    private String address;
    private String city;
    private Double latitude;
    private Double longitude;

    @Column(name = "location_pinned", nullable = false)
    @Builder.Default
    private Boolean locationPinned = false;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(columnDefinition = "TEXT[]")
    private String[] images;

    @Column(name = "ai_score")
    private Integer aiScore;

    @Column(name = "token_cost", nullable = false)
    @Builder.Default
    private Integer tokenCost = 0;

    @Column(length = 30)
    @Builder.Default
    private String status = "PENDING_APPROVAL";

    @Column(name = "selected_handyman_id")
    private UUID selectedHandymanId;

    @Column(name = "completed_at")
    private Instant completedAt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;
}
