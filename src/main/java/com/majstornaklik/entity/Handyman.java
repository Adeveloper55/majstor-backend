package com.majstornaklik.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "handymen")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Handyman {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "full_name", nullable = false, length = 100)
    private String fullName;

    @Column(nullable = false, unique = true, length = 100)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    private String phone;
    private String city;
    private Double latitude;
    private Double longitude;
    private String bio;

    @Column(name = "profile_image_url")
    private String profileImageUrl;

    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "is_verified")
    @Builder.Default
    private Boolean isVerified = false;

    @Column(name = "token_balance")
    @Builder.Default
    private Integer tokenBalance = 0;

    @Column(name = "average_rating")
    @Builder.Default
    private Double averageRating = 0.0;

    @Column(name = "total_reviews")
    @Builder.Default
    private Integer totalReviews = 0;

    @Column(name = "company_name", length = 200)
    private String companyName;

    @Column(length = 9)
    private String pib;

    private String address;

    @Column(name = "postal_code", length = 20)
    private String postalCode;

    private String country;

    @Column(name = "contact_person", length = 100)
    private String contactPerson;

    @Column(name = "is_company")
    @Builder.Default
    private Boolean isCompany = false;

    @Column(name = "coverage_districts", columnDefinition = "TEXT")
    private String coverageDistrictsJson;

    @Column(name = "service_categories", columnDefinition = "TEXT")
    private String serviceCategoriesJson;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;
}
