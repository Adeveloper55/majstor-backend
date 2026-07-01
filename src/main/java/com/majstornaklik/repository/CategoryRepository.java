package com.majstornaklik.repository;

import com.majstornaklik.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CategoryRepository extends JpaRepository<Category, Integer> {
    List<Category> findAllByOrderByNameAsc();
    Optional<Category> findBySlug(String slug);
}
