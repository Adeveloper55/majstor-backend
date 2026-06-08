package com.majstornaklik.repository;

import com.majstornaklik.entity.EmailVerificationToken;
import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;
import java.util.UUID;

public interface EmailVerificationTokenRepository extends JpaRepository<EmailVerificationToken, UUID> {
    Optional<EmailVerificationToken> findByTokenAndUsedFalse(String token);

    Optional<EmailVerificationToken> findByToken(String token);

    @Modifying
    @Query("UPDATE EmailVerificationToken t SET t.used = true WHERE t.email = :email AND t.used = false")
    void invalidateUnusedForEmail(String email);
}
