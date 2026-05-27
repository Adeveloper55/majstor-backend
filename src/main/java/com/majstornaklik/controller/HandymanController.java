package com.majstornaklik.controller;

import com.majstornaklik.dto.DtoMapper;
import com.majstornaklik.entity.Handyman;
import com.majstornaklik.repository.HandymanRepository;
import com.majstornaklik.security.SecurityUtils;
import com.majstornaklik.service.ApplicationService;
import com.majstornaklik.service.JobService;
import com.majstornaklik.service.TokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

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
        if (body.containsKey("phone")) h.setPhone((String) body.get("phone"));
        if (body.containsKey("city")) h.setCity((String) body.get("city"));
        if (body.containsKey("bio")) h.setBio((String) body.get("bio"));
        if (body.containsKey("profileImageUrl")) h.setProfileImageUrl((String) body.get("profileImageUrl"));
        if (body.containsKey("latitude")) h.setLatitude(toDouble(body.get("latitude")));
        if (body.containsKey("longitude")) h.setLongitude(toDouble(body.get("longitude")));
        return DtoMapper.toHandymanDto(handymanRepository.save(h));
    }

    private Double toDouble(Object value) {
        if (value == null) return null;
        if (value instanceof Number n) return n.doubleValue();
        return Double.parseDouble(value.toString());
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
