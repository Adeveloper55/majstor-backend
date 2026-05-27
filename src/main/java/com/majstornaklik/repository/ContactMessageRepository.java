package com.majstornaklik.repository;

import com.majstornaklik.entity.ContactMessage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ContactMessageRepository extends JpaRepository<ContactMessage, UUID> {

    Page<ContactMessage> findAllByOrderByCreatedAtDesc(Pageable pageable);

    Page<ContactMessage> findByIsContractorTrueOrderByCreatedAtDesc(Pageable pageable);

    long countByStatus(String status);
}
