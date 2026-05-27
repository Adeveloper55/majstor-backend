package com.majstornaklik.repository;

import com.majstornaklik.entity.TokenTransaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface TokenTransactionRepository extends JpaRepository<TokenTransaction, UUID> {
    List<TokenTransaction> findByHandymanIdOrderByCreatedAtDesc(UUID handymanId);
}
