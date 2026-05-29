package com.majstornaklik.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "company_registration_requests")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CompanyRegistrationRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 100)
    private String email;

    @Column(nullable = false, length = 30)
    private String phone;

    @Column(name = "normalized_phone", nullable = false, length = 30)
    private String normalizedPhone;

    @Column(name = "selected_service_ids", nullable = false, columnDefinition = "TEXT")
    private String selectedServiceIds;

    @Column(name = "selected_service_names", nullable = false, columnDefinition = "TEXT")
    private String selectedServiceNames;

    @Column(name = "company_short_description", length = 100)
    private String companyShortDescription;

    @Column(name = "selected_districts", nullable = false, columnDefinition = "TEXT")
    private String selectedDistricts;

    @Column(name = "company_name", nullable = false, length = 200)
    private String companyName;

    @Column(nullable = false, length = 9)
    private String pib;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String address;

    @Column(name = "postal_code", nullable = false, length = 20)
    private String postalCode;

    @Column(nullable = false, length = 100)
    private String city;

    @Column(nullable = false, length = 50)
    @Builder.Default
    private String country = "Srbija";

    @Column(name = "contact_person", nullable = false, length = 100)
    private String contactPerson;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(nullable = false, length = 20)
    @Builder.Default
    private String status = "PENDING";

    @Column(name = "admin_note", columnDefinition = "TEXT")
    private String adminNote;

    @Column(name = "handyman_id")
    private UUID handymanId;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @Column(name = "reviewed_at")
    private Instant reviewedAt;
}
