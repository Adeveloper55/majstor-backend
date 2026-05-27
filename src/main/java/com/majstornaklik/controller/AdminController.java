package com.majstornaklik.controller;

import com.majstornaklik.dto.AdminCreateHandymanRequest;
import com.majstornaklik.dto.AdminCreateJobRequest;
import com.majstornaklik.dto.AdjustTokensRequest;
import com.majstornaklik.dto.DtoMapper;
import com.majstornaklik.dto.ContactMessageDto;
import com.majstornaklik.dto.RejectTokenRequest;
import com.majstornaklik.entity.Review;
import com.majstornaklik.service.AdminService;
import com.majstornaklik.service.ApplicationService;
import com.majstornaklik.service.ContactService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;
    private final ContactService contactService;
    private final ApplicationService applicationService;

    @GetMapping("/users")
    public Page<DtoMapper.UserDto> listUsers(@RequestParam(required = false) String search, Pageable pageable) {
        return adminService.listUsers(search, pageable);
    }

    @GetMapping("/users/{id}")
    public DtoMapper.UserDto getUser(@PathVariable UUID id) {
        return adminService.getUser(id);
    }

    @DeleteMapping("/users/{id}")
    public void deactivateUser(@PathVariable UUID id) {
        adminService.deactivateUser(id);
    }

    @GetMapping("/handymen")
    public Page<DtoMapper.HandymanDto> listHandymen(@RequestParam(required = false) String search, Pageable pageable) {
        return adminService.listHandymen(search, pageable);
    }

    @PostMapping("/handymen")
    public DtoMapper.HandymanDto createHandyman(@Valid @RequestBody AdminCreateHandymanRequest req) {
        return adminService.createHandyman(req);
    }

    @GetMapping("/handymen/{id}")
    public DtoMapper.HandymanDto getHandyman(@PathVariable UUID id) {
        return adminService.getHandyman(id);
    }

    @DeleteMapping("/handymen/{id}")
    public void deactivateHandyman(@PathVariable UUID id) {
        adminService.deactivateHandyman(id);
    }

    @PostMapping("/handymen/{id}/adjust-tokens")
    public void adjustTokens(@PathVariable UUID id, @Valid @RequestBody AdjustTokensRequest req) {
        adminService.adjustTokens(id, req.amount(), req.description());
    }

    @GetMapping("/jobs")
    public Page<DtoMapper.JobListingDto> listJobs(Pageable pageable) {
        return adminService.listJobs(pageable);
    }

    @PostMapping("/jobs")
    public DtoMapper.JobListingDto createJob(@Valid @RequestBody AdminCreateJobRequest req) {
        return adminService.createJob(req);
    }

    @GetMapping("/jobs/{id}")
    public DtoMapper.JobListingDto getJob(@PathVariable UUID id) {
        return adminService.getJob(id);
    }

    @DeleteMapping("/jobs/{id}")
    public void deleteJob(@PathVariable UUID id) {
        adminService.deleteJob(id);
    }

    @GetMapping("/jobs/{id}/applications")
    public Page<Map<String, Object>> listJobApplications(@PathVariable UUID id, Pageable pageable) {
        return applicationService.listForJobAdmin(id, pageable);
    }

    @PostMapping("/jobs/{id}/assign/{handymanId}")
    public void assignHandyman(@PathVariable UUID id, @PathVariable UUID handymanId) {
        applicationService.assignHandymanByAdmin(id, handymanId);
    }

    @GetMapping("/job-applications")
    public Page<Map<String, Object>> listJobApplications(
            @RequestParam(required = false, defaultValue = "PENDING") String status,
            Pageable pageable) {
        if ("PENDING".equals(status)) {
            return applicationService.listPendingForAdmin(pageable);
        }
        return Page.empty(pageable);
    }

    @PostMapping("/job-applications/{applicationId}/assign")
    public void assignByApplication(@PathVariable UUID applicationId) {
        applicationService.assignByApplicationId(applicationId);
    }

    @GetMapping("/token-requests")
    public Page<Map<String, Object>> listTokenRequests(
            @RequestParam(required = false) String status, Pageable pageable) {
        return adminService.listTokenRequests(status, pageable);
    }

    @PostMapping("/token-requests/{id}/approve")
    public void approveTokenRequest(@PathVariable UUID id) {
        adminService.approveTokenRequest(id);
    }

    @PostMapping("/token-requests/{id}/reject")
    public void rejectTokenRequest(@PathVariable UUID id, @RequestBody(required = false) RejectTokenRequest req) {
        adminService.rejectTokenRequest(id, req != null ? req.adminNote() : null);
    }

    @GetMapping("/stats")
    public Map<String, Object> stats() {
        return adminService.getStats();
    }

    @GetMapping("/reviews")
    public Page<Review> listReviews(Pageable pageable) {
        return adminService.listReviews(pageable);
    }

    @DeleteMapping("/reviews/{id}")
    public void deleteReview(@PathVariable UUID id) {
        adminService.deleteReview(id);
    }

    @GetMapping("/contact-messages")
    public Page<ContactMessageDto> listContactMessages(Pageable pageable) {
        return contactService.listAll(pageable);
    }

    @GetMapping("/contact-messages/{id}")
    public ContactMessageDto getContactMessage(@PathVariable UUID id) {
        return contactService.get(id);
    }

    @PostMapping("/contact-messages/{id}/read")
    public ContactMessageDto markContactMessageRead(@PathVariable UUID id) {
        return contactService.markRead(id);
    }

    @DeleteMapping("/contact-messages/{id}")
    public void deleteContactMessage(@PathVariable UUID id) {
        contactService.delete(id);
    }
}
