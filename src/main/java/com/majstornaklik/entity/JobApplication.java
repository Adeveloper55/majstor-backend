package com.majstornaklik.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "job_applications")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JobApplication {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "job_listing_id", nullable = false)
    private UUID jobListingId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "handyman_id", nullable = false)
    private Handyman handyman;

    @Column(name = "tokens_spent", nullable = false)
    private Integer tokensSpent;

    @Column(name = "cover_message", columnDefinition = "TEXT")
    private String coverMessage;

    @Column(length = 30)
    @Builder.Default
    private String status = "PENDING";

    @CreationTimestamp
    @Column(name = "applied_at", updatable = false)
    private Instant appliedAt;
}
