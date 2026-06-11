package com.majstornaklik.repository;

import com.majstornaklik.entity.TokenPurchaseRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface TokenPurchaseRequestRepository extends JpaRepository<TokenPurchaseRequest, UUID> {
    List<TokenPurchaseRequest> findByHandymanIdOrderByCreatedAtDesc(UUID handymanId);
    Page<TokenPurchaseRequest> findByHandymanIdOrderByCreatedAtDesc(UUID handymanId, Pageable pageable);
    Page<TokenPurchaseRequest> findByStatus(String status, Pageable pageable);
    Page<TokenPurchaseRequest> findAllByOrderByCreatedAtDesc(Pageable pageable);

    @Query(value = "SELECT r FROM TokenPurchaseRequest r JOIN FETCH r.handyman ORDER BY r.createdAt DESC",
            countQuery = "SELECT COUNT(r) FROM TokenPurchaseRequest r")
    Page<TokenPurchaseRequest> findAllWithHandyman(Pageable pageable);

    @Query(value = "SELECT r FROM TokenPurchaseRequest r JOIN FETCH r.handyman WHERE r.status = :status ORDER BY r.createdAt DESC",
            countQuery = "SELECT COUNT(r) FROM TokenPurchaseRequest r WHERE r.status = :status")
    Page<TokenPurchaseRequest> findByStatusWithHandyman(@Param("status") String status, Pageable pageable);
}
