package com.majstornaklik.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "token_purchase_requests")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TokenPurchaseRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "handyman_id", nullable = false)
    private Handyman handyman;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "package_id")
    private TokenPackage tokenPackage;

    @Column(name = "token_amount", nullable = false)
    private Integer tokenAmount;

    @Column(name = "amount_expected", nullable = false, precision = 10, scale = 2)
    private BigDecimal amountExpected;

    @Column(name = "payment_reference")
    private String paymentReference;

    @Column(length = 30)
    @Builder.Default
    private String status = "PENDING";

    @Column(name = "admin_note", columnDefinition = "TEXT")
    private String adminNote;

    @Column(name = "processed_at")
    private Instant processedAt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;
}
