package com.majstornaklik.repository;

import com.majstornaklik.entity.TokenPurchaseRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface TokenPurchaseRequestRepository extends JpaRepository<TokenPurchaseRequest, UUID> {
    List<TokenPurchaseRequest> findByHandymanIdOrderByCreatedAtDesc(UUID handymanId);
    Page<TokenPurchaseRequest> findByHandymanIdOrderByCreatedAtDesc(UUID handymanId, Pageable pageable);
    Page<TokenPurchaseRequest> findByStatus(String status, Pageable pageable);
    Page<TokenPurchaseRequest> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
