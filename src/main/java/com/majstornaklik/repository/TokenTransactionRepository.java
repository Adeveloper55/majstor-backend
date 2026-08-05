package com.majstornaklik.repository;

import com.majstornaklik.entity.TokenTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface TokenTransactionRepository extends JpaRepository<TokenTransaction, UUID> {
    List<TokenTransaction> findByHandymanIdOrderByCreatedAtDesc(UUID handymanId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM TokenTransaction t WHERE t.handymanId = :handymanId")
    int deleteByHandymanId(@Param("handymanId") UUID handymanId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM TokenTransaction t WHERE t.jobApplicationId IN :applicationIds")
    int deleteByJobApplicationIdIn(@Param("applicationIds") Collection<UUID> applicationIds);
}
