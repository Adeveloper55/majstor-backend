package com.majstornaklik.controller;

import com.majstornaklik.dto.SubmitReviewRequest;
import com.majstornaklik.entity.Review;
import com.majstornaklik.security.SecurityUtils;
import com.majstornaklik.service.ReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;
    private final SecurityUtils securityUtils;

    @PostMapping
    public Map<String, Object> submit(@Valid @RequestBody SubmitReviewRequest req) {
        var user = securityUtils.getCurrentUser();
        return reviewService.submitReview(user.getId(), user.getRole(), req);
    }

    @GetMapping("/job/{jobId}/status")
    public Map<String, Object> reviewStatus(@PathVariable UUID jobId) {
        var user = securityUtils.getCurrentUser();
        return reviewService.getReviewStatus(jobId, user.getId(), user.getRole());
    }

    @GetMapping("/user/{id}")
    public Page<Review> forUser(@PathVariable UUID id, Pageable pageable) {
        return reviewService.getReviewsForUser(id, pageable);
    }

    @GetMapping("/handyman/{id}")
    public Page<Review> forHandyman(@PathVariable UUID id, Pageable pageable) {
        return reviewService.getReviewsForHandyman(id, pageable);
    }
}
