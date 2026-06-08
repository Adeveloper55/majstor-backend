package com.majstornaklik.repository;

import com.majstornaklik.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
    boolean existsByPhoneNormalized(String phoneNormalized);
    boolean existsByPhoneNormalizedAndIdNot(String phoneNormalized, UUID id);
    Page<User> findByFullNameContainingIgnoreCaseOrEmailContainingIgnoreCase(String fullName, String email, Pageable pageable);
}
