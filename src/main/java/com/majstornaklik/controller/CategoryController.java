package com.majstornaklik.controller;

import com.majstornaklik.dto.DtoMapper;
import com.majstornaklik.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
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
        var all = categoryRepository.findAllByOrderByNameAsc().stream()
                .map(DtoMapper::toCategoryDto)
                .toList();
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), all.size());
        if (start >= all.size()) {
            return new PageImpl<>(java.util.List.of(), pageable, all.size());
        }
        return new PageImpl<>(all.subList(start, end), pageable, all.size());
    }
}
