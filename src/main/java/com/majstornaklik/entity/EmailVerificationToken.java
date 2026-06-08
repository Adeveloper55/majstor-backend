package com.majstornaklik.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "email_verification_tokens")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmailVerificationToken {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 100)
    private String email;

    @Column(nullable = false, unique = true)
    private String token;

    @Column(nullable = false, length = 40)
    private String role;

    @Column(name = "reference_id")
    private UUID referenceId;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Builder.Default
    private Boolean used = false;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;
}
