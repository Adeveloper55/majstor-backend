package com.majstornaklik.repository;

import com.majstornaklik.entity.TokenPackage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface TokenPackageRepository extends JpaRepository<TokenPackage, Integer> {
    List<TokenPackage> findByIsActiveTrue();
    Page<TokenPackage> findByIsActiveTrue(Pageable pageable);
}
