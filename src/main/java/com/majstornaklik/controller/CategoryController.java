package com.majstornaklik.controller;

import com.majstornaklik.dto.DtoMapper;
import com.majstornaklik.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryRepository categoryRepository;

    @GetMapping
    public Page<DtoMapper.CategoryDto> list(Pageable pageable) {
        return categoryRepository.findAll(pageable).map(DtoMapper::toCategoryDto);
    }
}
