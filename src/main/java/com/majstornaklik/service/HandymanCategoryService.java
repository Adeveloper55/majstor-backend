package com.majstornaklik.service;

import com.majstornaklik.entity.Category;
import com.majstornaklik.entity.Handyman;
import com.majstornaklik.repository.CategoryRepository;
import com.majstornaklik.util.JsonUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class HandymanCategoryService {

    public static final int MAX_CATEGORIES = 10;

    private final CategoryRepository categoryRepository;

    public List<Integer> getCategoryIds(Handyman handyman) {
        return JsonUtils.parseIntegerList(handyman.getCategoryIdsJson());
    }

    public List<Integer> validateCategoryIds(List<Integer> categoryIds) {
        if (categoryIds == null || categoryIds.isEmpty()) {
            throw new IllegalArgumentException("Izaberite bar jednu kategoriju posla.");
        }
        if (categoryIds.size() > MAX_CATEGORIES) {
            throw new IllegalArgumentException("Možete izabrati najviše " + MAX_CATEGORIES + " kategorija.");
        }

        Set<Integer> unique = new LinkedHashSet<>();
        for (Integer id : categoryIds) {
            if (id == null) {
                throw new IllegalArgumentException("Neispravna kategorija.");
            }
            if (!unique.add(id)) {
                throw new IllegalArgumentException("Duplikat kategorije nije dozvoljen.");
            }
            if (!categoryRepository.existsById(id)) {
                throw new IllegalArgumentException("Kategorija nije pronađena.");
            }
        }
        return List.copyOf(unique);
    }

    public String toJson(List<Integer> categoryIds) {
        return JsonUtils.toJsonIntegers(validateCategoryIds(categoryIds));
    }

    public void assertCanAccessJobCategory(Handyman handyman, Integer jobCategoryId) {
        List<Integer> allowed = getCategoryIds(handyman);
        if (allowed.isEmpty()) {
            throw new IllegalArgumentException("Niste izabrali kategorije poslova na profilu.");
        }
        if (!allowed.contains(jobCategoryId)) {
            throw new IllegalArgumentException("Ovaj posao nije u vašim kategorijama.");
        }
    }

    public List<Integer> resolveCategoryIdsFromNames(List<String> serviceNames) {
        if (serviceNames == null || serviceNames.isEmpty()) {
            return List.of();
        }
        List<Category> all = categoryRepository.findAll();
        Set<Integer> ids = new LinkedHashSet<>();

        for (String serviceName : serviceNames) {
            if (ids.size() >= MAX_CATEGORIES) break;
            Integer match = findBestCategoryId(normalize(serviceName), all);
            if (match != null) {
                ids.add(match);
            }
        }
        return List.copyOf(ids);
    }

    private Integer findBestCategoryId(String normalizedService, List<Category> categories) {
        Integer exact = null;
        Integer contains = null;
        for (Category c : categories) {
            String normalizedCategory = normalize(c.getName());
            if (normalizedService.equals(normalizedCategory)) {
                exact = c.getId();
                break;
            }
            if (contains == null && (normalizedCategory.contains(normalizedService)
                    || normalizedService.contains(normalizedCategory))) {
                contains = c.getId();
            }
        }
        return exact != null ? exact : contains;
    }

    private String normalize(String value) {
        if (value == null) return "";
        String n = Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT);
        return n.replaceAll("[^a-z0-9]+", " ").trim();
    }
}
