package com.majstornaklik.repository;

import com.majstornaklik.entity.ServiceInquiry;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ServiceInquiryRepository extends JpaRepository<ServiceInquiry, UUID> {

    Page<ServiceInquiry> findAllByOrderByCreatedAtDesc(Pageable pageable);

    long countByStatus(String status);
}
