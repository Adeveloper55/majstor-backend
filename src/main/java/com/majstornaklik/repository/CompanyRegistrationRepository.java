package com.majstornaklik.repository;

import com.majstornaklik.entity.CompanyRegistrationRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface CompanyRegistrationRepository extends JpaRepository<CompanyRegistrationRequest, UUID> {

    Page<CompanyRegistrationRequest> findByStatusOrderByCreatedAtDesc(String status, Pageable pageable);

    Page<CompanyRegistrationRequest> findAllByOrderByCreatedAtDesc(Pageable pageable);

    boolean existsByEmailAndStatus(String email, String status);

    long countByStatus(String status);
}
