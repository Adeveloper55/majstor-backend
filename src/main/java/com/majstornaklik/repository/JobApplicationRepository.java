package com.majstornaklik.repository;

import com.majstornaklik.entity.JobApplication;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface JobApplicationRepository extends JpaRepository<JobApplication, UUID> {
    List<JobApplication> findByJobListingId(UUID jobListingId);
    List<JobApplication> findByHandymanId(UUID handymanId);
    Page<JobApplication> findByJobListingId(UUID jobListingId, Pageable pageable);
    Page<JobApplication> findByHandymanIdOrderByAppliedAtDesc(UUID handymanId, Pageable pageable);
    Optional<JobApplication> findByJobListingIdAndHandymanId(UUID jobListingId, UUID handymanId);
    boolean existsByJobListingIdAndHandymanId(UUID jobListingId, UUID handymanId);

    @Query("SELECT a FROM JobApplication a WHERE a.handyman.id = :handymanId ORDER BY a.appliedAt DESC")
    Page<JobApplication> findByHandymanIdWithHandyman(@Param("handymanId") UUID handymanId, Pageable pageable);

    Page<JobApplication> findByStatusOrderByAppliedAtDesc(String status, Pageable pageable);

    long countByStatus(String status);

    @Query("SELECT a FROM JobApplication a JOIN FETCH a.handyman WHERE a.handyman.id = :handymanId AND a.status IN ('UNLOCKED', 'ACCEPTED') ORDER BY a.appliedAt DESC")
    List<JobApplication> findUnlockedByHandymanId(@Param("handymanId") UUID handymanId);

    @Query("SELECT a FROM JobApplication a WHERE a.handyman.id = :handymanId AND a.status IN ('UNLOCKED', 'ACCEPTED') ORDER BY a.appliedAt DESC")
    Page<JobApplication> findUnlockedByHandymanId(@Param("handymanId") UUID handymanId, Pageable pageable);
}
