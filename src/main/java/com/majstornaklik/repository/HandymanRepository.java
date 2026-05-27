package com.majstornaklik.repository;

import com.majstornaklik.entity.Handyman;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface HandymanRepository extends JpaRepository<Handyman, UUID> {
    Optional<Handyman> findByEmail(String email);
    boolean existsByEmail(String email);
    Page<Handyman> findByFullNameContainingIgnoreCaseOrEmailContainingIgnoreCase(String fullName, String email, Pageable pageable);
}
