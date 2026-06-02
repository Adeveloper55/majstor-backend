package com.majstornaklik.controller;

import com.majstornaklik.dto.*;
import com.majstornaklik.entity.Handyman;
import com.majstornaklik.repository.*;
import com.majstornaklik.security.SecurityUtils;
import com.majstornaklik.service.ApplicationService;
import com.majstornaklik.service.HandymanCategoryService;
import com.majstornaklik.service.JobService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/jobs")
@RequiredArgsConstructor
public class JobController {

    private final JobService jobService;
    private final ApplicationService applicationService;
    private final SecurityUtils securityUtils;
    private final HandymanRepository handymanRepository;
    private final HandymanCategoryService handymanCategoryService;

    @GetMapping
    public Page<DtoMapper.JobListingDto> listJobs(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Integer category,
            @RequestParam(required = false) List<Integer> categories,
            @RequestParam(required = false) String city,
            @RequestParam(required = false) Double lat,
            @RequestParam(required = false) Double lon,
            @RequestParam(required = false) Double radius,
            @RequestParam(required = false) Integer minTokenCost,
            @RequestParam(required = false) Integer maxTokenCost,
            @RequestParam(required = false, defaultValue = "newest") String sort,
            Pageable pageable) {
        List<Integer> restrictedCategories = null;
        var viewer = securityUtils.getCurrentUserOrNull();
        if (viewer != null && "ROLE_HANDYMAN".equals(viewer.getRole())) {
            Handyman handyman = handymanRepository.findById(viewer.getId())
                    .orElseThrow(() -> new IllegalArgumentException("Majstor nije pronađen"));
            restrictedCategories = handymanCategoryService.getCategoryIds(handyman);
        }
        return jobService.listJobs(status, category, categories, city, lat, lon, radius,
                minTokenCost, maxTokenCost, sort, pageable, restrictedCategories);
    }

    @GetMapping("/{id}")
    public DtoMapper.JobListingDto getJob(
            @PathVariable UUID id,
            @RequestParam(required = false) Double lat,
            @RequestParam(required = false) Double lon,
            @RequestParam(required = false) Double radius) {
        var viewer = securityUtils.getCurrentUserOrNull();
        UUID viewerId = viewer != null ? viewer.getId() : null;
        String viewerRole = viewer != null ? viewer.getRole() : null;
        return jobService.getJob(id, lat, lon, radius, viewerId, viewerRole);
    }

    @PostMapping
    public DtoMapper.JobListingDto createJob(@Valid @RequestBody CreateJobRequest req) {
        securityUtils.requireRole("ROLE_CLIENT");
        return jobService.createJob(securityUtils.getCurrentUserId(), req);
    }

    @PutMapping("/{id}")
    public DtoMapper.JobListingDto updateJob(@PathVariable UUID id, @Valid @RequestBody CreateJobRequest req) {
        securityUtils.requireRole("ROLE_CLIENT");
        return jobService.updateJob(securityUtils.getCurrentUserId(), id, req);
    }

    @DeleteMapping("/{id}")
    public void cancelJob(@PathVariable UUID id) {
        securityUtils.requireRole("ROLE_CLIENT");
        jobService.cancelJob(securityUtils.getCurrentUserId(), id);
    }

    @PostMapping("/{id}/complete")
    public DtoMapper.JobListingDto completeJob(@PathVariable UUID id) {
        var user = securityUtils.getCurrentUser();
        return jobService.completeJob(user.getId(), user.getRole(), id);
    }

    @GetMapping("/my/recent-applications")
    public List<Map<String, Object>> recentApplications() {
        securityUtils.requireRole("ROLE_CLIENT");
        return applicationService.listRecentForClient(securityUtils.getCurrentUserId());
    }

    @GetMapping("/my")
    public Page<DtoMapper.JobListingDto> myJobs(Pageable pageable) {
        securityUtils.requireRole("ROLE_CLIENT");
        return jobService.getMyJobs(securityUtils.getCurrentUserId(), pageable);
    }

    @PostMapping("/score-preview")
    public ScorePreviewResponse scorePreview(@Valid @RequestBody ScorePreviewRequest req) {
        securityUtils.requireRole("ROLE_CLIENT");
        return jobService.scorePreview(req);
    }

    @PostMapping("/{id}/apply")
    public Map<String, Object> apply(@PathVariable UUID id, @RequestBody(required = false) ApplyJobRequest req) {
        securityUtils.requireRole("ROLE_HANDYMAN");
        return applicationService.apply(securityUtils.getCurrentUserId(), id, req);
    }

    @GetMapping("/{id}/applications")
    public Page<Map<String, Object>> listApplications(@PathVariable UUID id, Pageable pageable) {
        securityUtils.requireRole("ROLE_CLIENT");
        return applicationService.listForJob(securityUtils.getCurrentUserId(), id, pageable);
    }
}
