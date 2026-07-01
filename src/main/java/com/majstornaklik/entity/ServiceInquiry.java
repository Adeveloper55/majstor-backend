package com.majstornaklik.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "service_inquiries")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ServiceInquiry {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "category_slug", nullable = false, length = 120)
    private String categorySlug;

    @Column(name = "category_name", nullable = false, length = 200)
    private String categoryName;

    @Column(nullable = false, length = 100)
    private String city;

    @Column(name = "start_timeline", nullable = false, length = 80)
    private String startTimeline;

    @Column(name = "short_description", length = 120)
    private String shortDescription;

    @Column(name = "detailed_description", nullable = false, columnDefinition = "TEXT")
    private String detailedDescription;

    @Column(length = 10)
    private String salutation;

    @Column(name = "full_name", nullable = false, length = 150)
    private String fullName;

    @Column(nullable = false, length = 100)
    private String email;

    @Column(length = 30)
    private String phone;

    @Column(length = 20)
    @Builder.Default
    private String status = "NEW";

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;
}
