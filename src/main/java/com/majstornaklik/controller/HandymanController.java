package com.majstornaklik.controller;

import com.majstornaklik.dto.DtoMapper;
import com.majstornaklik.entity.Handyman;
import com.majstornaklik.repository.HandymanRepository;
import com.majstornaklik.security.SecurityUtils;
import com.majstornaklik.service.ApplicationService;
import com.majstornaklik.service.HandymanCategoryService;
import com.majstornaklik.service.JobService;
import com.majstornaklik.service.PhoneUniquenessService;
import com.majstornaklik.service.TokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

import com.majstornaklik.util.PibUtils;
import com.majstornaklik.util.PhoneUtils;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/handymen")
@RequiredArgsConstructor
public class HandymanController {

    private final HandymanRepository handymanRepository;
    private final SecurityUtils securityUtils;
    private final TokenService tokenService;
    private final ApplicationService applicationService;
    private final JobService jobService;
    private final HandymanCategoryService handymanCategoryService;
    private final PhoneUniquenessService phoneUniquenessService;

    @GetMapping("/me")
    public DtoMapper.HandymanDto getMe() {
        securityUtils.requireRole("ROLE_HANDYMAN");
        return DtoMapper.toHandymanDto(getCurrent());
    }

    @PutMapping("/me")
    public DtoMapper.HandymanDto updateMe(@RequestBody Map<String, Object> body) {
        securityUtils.requireRole("ROLE_HANDYMAN");
        Handyman h = getCurrent();
        if (body.containsKey("fullName")) h.setFullName((String) body.get("fullName"));
        if (body.containsKey("phone")) {
            String phoneNormalized = PhoneUtils.normalizeOptional((String) body.get("phone"));
            phoneUniquenessService.assertPhoneAvailable(phoneNormalized, null, h.getId(), null);
            h.setPhone(phoneNormalized);
            h.setPhoneNormalized(phoneNormalized);
        }
        if (body.containsKey("city")) h.setCity((String) body.get("city"));
        if (body.containsKey("bio")) h.setBio((String) body.get("bio"));
        if (body.containsKey("profileImageUrl")) h.setProfileImageUrl((String) body.get("profileImageUrl"));
        if (body.containsKey("latitude")) h.setLatitude(toDouble(body.get("latitude")));
        if (body.containsKey("longitude")) h.setLongitude(toDouble(body.get("longitude")));
        if (body.containsKey("companyName")) h.setCompanyName(trimOrNull((String) body.get("companyName")));
        if (body.containsKey("address")) h.setAddress(trimOrNull((String) body.get("address")));
        if (body.containsKey("postalCode")) h.setPostalCode(trimOrNull((String) body.get("postalCode")));
        if (body.containsKey("country")) h.setCountry(trimOrNull((String) body.get("country")));
        if (body.containsKey("contactPerson")) h.setContactPerson(trimOrNull((String) body.get("contactPerson")));
        if (body.containsKey("pib")) {
            String pib = PibUtils.normalizeOptional((String) body.get("pib"));
            if (pib != null && handymanRepository.existsByPibAndIdNot(pib, h.getId())) {
                throw new IllegalArgumentException("PIB je već registrovan");
            }
            h.setPib(pib);
        }
        if (body.containsKey("categoryIds")) {
            h.setCategoryIdsJson(handymanCategoryService.toJson(parseCategoryIds(body.get("categoryIds"))));
        }
        return DtoMapper.toHandymanDto(handymanRepository.save(h));
    }

    private List<Integer> parseCategoryIds(Object value) {
        if (!(value instanceof List<?> list)) {
            throw new IllegalArgumentException("categoryIds mora biti lista");
        }
        return list.stream()
                .map(item -> item instanceof Number n ? n.intValue() : Integer.parseInt(item.toString()))
                .toList();
    }

    private Double toDouble(Object value) {
        if (value == null) return null;
        if (value instanceof Number n) return n.doubleValue();
        return Double.parseDouble(value.toString());
    }

    private String trimOrNull(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    @GetMapping("/{id}")
    public DtoMapper.HandymanPublicDto getPublic(@PathVariable UUID id) {
        Handyman h = handymanRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Majstor nije pronađen"));
        return DtoMapper.toHandymanPublicDto(h);
    }

    @GetMapping("/me/tokens")
    public Map<String, Object> getTokens() {
        securityUtils.requireRole("ROLE_HANDYMAN");
        return tokenService.getTokenInfo(securityUtils.getCurrentUserId());
    }

    @GetMapping("/me/applications")
    public Page<Map<String, Object>> getApplications(Pageable pageable) {
        securityUtils.requireRole("ROLE_HANDYMAN");
        return applicationService.listForHandyman(securityUtils.getCurrentUserId(), pageable);
    }

    @GetMapping("/me/assigned-jobs")
    public java.util.List<DtoMapper.JobListingDto> getAssignedJobs() {
        securityUtils.requireRole("ROLE_HANDYMAN");
        return jobService.getAssignedJobsForHandyman(securityUtils.getCurrentUserId());
    }

    @GetMapping("/me/available-jobs")
    public Page<DtoMapper.JobListingDto> getAvailableJobs(
            @RequestParam(required = false) List<Integer> categories,
            @RequestParam(required = false) String city,
            @RequestParam(required = false) Double lat,
            @RequestParam(required = false) Double lon,
            @RequestParam(required = false) Double radius,
            @RequestParam(required = false) Integer minTokenCost,
            @RequestParam(required = false) Integer maxTokenCost,
            @RequestParam(required = false, defaultValue = "newest") String sort,
            Pageable pageable) {
        securityUtils.requireRole("ROLE_HANDYMAN");
        return jobService.listAvailableJobsForHandyman(
                securityUtils.getCurrentUserId(),
                categories, city, lat, lon, radius,
                minTokenCost, maxTokenCost, sort, pageable);
    }

    @DeleteMapping("/me")
    public void deactivate() {
        securityUtils.requireRole("ROLE_HANDYMAN");
        Handyman h = getCurrent();
        h.setIsActive(false);
        handymanRepository.save(h);
    }

    private Handyman getCurrent() {
        return handymanRepository.findById(securityUtils.getCurrentUserId())
                .orElseThrow(() -> new IllegalArgumentException("Majstor nije pronađen"));
    }
}
