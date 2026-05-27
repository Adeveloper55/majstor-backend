package com.majstornaklik.repository;

import com.majstornaklik.entity.JobListing;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface JobListingRepository extends JpaRepository<JobListing, UUID> {
    Page<JobListing> findByUserId(UUID userId, Pageable pageable);
    Page<JobListing> findByStatus(String status, Pageable pageable);

    @Query("SELECT j FROM JobListing j JOIN FETCH j.category WHERE j.status = :status AND (:categoryId IS NULL OR j.category.id = :categoryId) AND (:city IS NULL OR :city = '' OR LOWER(j.city) LIKE LOWER(CONCAT('%', :city, '%')))")
    Page<JobListing> findWithFilters(@Param("status") String status, @Param("categoryId") Integer categoryId, @Param("city") String city, Pageable pageable);

    @Query("SELECT j FROM JobListing j JOIN FETCH j.category WHERE j.status = :status AND (:categoryId IS NULL OR j.category.id = :categoryId) AND (:city IS NULL OR :city = '' OR LOWER(j.city) LIKE LOWER(CONCAT('%', :city, '%')))")
    List<JobListing> findAllWithFilters(@Param("status") String status, @Param("categoryId") Integer categoryId, @Param("city") String city);

    List<JobListing> findByUserIdAndStatusIn(UUID userId, List<String> statuses);

    @Query("SELECT j FROM JobListing j JOIN FETCH j.category WHERE j.id = :id")
    java.util.Optional<JobListing> findByIdWithCategory(@Param("id") UUID id);

    @Query("SELECT j FROM JobListing j JOIN FETCH j.category WHERE j.selectedHandymanId = :handymanId ORDER BY j.createdAt DESC")
    List<JobListing> findBySelectedHandymanIdWithCategory(@Param("handymanId") UUID handymanId);
}
