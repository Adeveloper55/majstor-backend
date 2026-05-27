package com.majstornaklik.service;

import com.majstornaklik.dto.ClientContactDto;
import com.majstornaklik.dto.CreateJobRequest;
import com.majstornaklik.dto.DtoMapper;
import com.majstornaklik.dto.ScorePreviewRequest;
import com.majstornaklik.dto.ScorePreviewResponse;
import com.majstornaklik.entity.Category;
import com.majstornaklik.entity.JobListing;
import com.majstornaklik.repository.CategoryRepository;
import com.majstornaklik.repository.HandymanRepository;
import com.majstornaklik.repository.JobListingRepository;
import com.majstornaklik.repository.UserRepository;
import com.majstornaklik.util.GeoUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class JobService {

    private final JobListingRepository jobListingRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final HandymanRepository handymanRepository;
    private final AiScoringService aiScoringService;
    private final EmailService emailService;

    @Value("${app.frontend.url:http://localhost:3000}")
    private String frontendUrl;

    public ScorePreviewResponse scorePreview(ScorePreviewRequest req) {
        Category category = categoryRepository.findById(req.categoryId())
                .orElseThrow(() -> new IllegalArgumentException("Kategorija nije pronađena"));
        var result = aiScoringService.scoreJob(category.getName(), req.description());
        int tokenCost = result.score() * category.getBaseTokenCost();
        return new ScorePreviewResponse(result.score(), result.reason(), tokenCost);
    }

    @Transactional
    public DtoMapper.JobListingDto createJob(UUID userId, CreateJobRequest req) {
        Category category = categoryRepository.findById(req.categoryId())
                .orElseThrow(() -> new IllegalArgumentException("Kategorija nije pronađena"));
        var aiResult = aiScoringService.scoreJob(category.getName(), req.description());
        int tokenCost = aiResult.score() * category.getBaseTokenCost();

        JobListing job = JobListing.builder()
                .userId(userId)
                .category(category)
                .title(req.title())
                .description(req.description())
                .address(req.address())
                .city(req.city())
                .latitude(req.latitude())
                .longitude(req.longitude())
                .images(req.images())
                .aiScore(aiResult.score())
                .tokenCost(tokenCost)
                .status("OPEN")
                .build();
        jobListingRepository.save(job);
        return DtoMapper.toJobDto(job, null);
    }

    @Transactional(readOnly = true)
    public DtoMapper.JobListingDto getJob(UUID id, Double userLat, Double userLon, Double radiusKm,
                                          UUID viewerId, String viewerRole) {
        JobListing job = jobListingRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Posao nije pronađen"));
        Double distance = computeDistance(job, userLat, userLon);
        if (radiusKm != null && distance != null && distance > radiusKm) {
            throw new IllegalArgumentException("Posao nije u zadatom radijusu");
        }
        ClientContactDto clientContact = resolveClientContact(job, viewerId, viewerRole);
        return DtoMapper.toJobDto(job, distance, clientContact);
    }

    @Transactional(readOnly = true)
    public List<DtoMapper.JobListingDto> getAssignedJobsForHandyman(UUID handymanId) {
        return jobListingRepository.findBySelectedHandymanIdWithCategory(handymanId).stream()
                .filter(j -> "IN_PROGRESS".equals(j.getStatus()) || "COMPLETED".equals(j.getStatus()))
                .map(j -> {
                    ClientContactDto contact = userRepository.findById(j.getUserId())
                            .map(DtoMapper::toClientContact)
                            .orElse(null);
                    return DtoMapper.toJobDto(j, null, contact);
                })
                .toList();
    }

    private ClientContactDto resolveClientContact(JobListing job, UUID viewerId, String viewerRole) {
        if (viewerId == null || viewerRole == null) {
            return null;
        }
        if ("ROLE_ADMIN".equals(viewerRole)) {
            return userRepository.findById(job.getUserId()).map(DtoMapper::toClientContact).orElse(null);
        }
        if ("ROLE_CLIENT".equals(viewerRole) && job.getUserId().equals(viewerId)) {
            return userRepository.findById(job.getUserId()).map(DtoMapper::toClientContact).orElse(null);
        }
        if ("ROLE_HANDYMAN".equals(viewerRole)
                && job.getSelectedHandymanId() != null
                && job.getSelectedHandymanId().equals(viewerId)
                && ("IN_PROGRESS".equals(job.getStatus()) || "COMPLETED".equals(job.getStatus()))) {
            return userRepository.findById(job.getUserId()).map(DtoMapper::toClientContact).orElse(null);
        }
        return null;
    }

    @Transactional(readOnly = true)
    public Page<DtoMapper.JobListingDto> listJobs(String status, Integer categoryId, List<Integer> categoryIds,
                                                   String city, Double userLat, Double userLon, Double radiusKm,
                                                   Integer minTokenCost, Integer maxTokenCost, String sort,
                                                   Pageable pageable) {
        String jobStatus = status != null ? status : "OPEN";
        List<Integer> filterCategories = categoryIds != null && !categoryIds.isEmpty()
                ? categoryIds
                : (categoryId != null ? List.of(categoryId) : null);

        boolean needsMemoryProcessing = filterCategories != null
                || minTokenCost != null || maxTokenCost != null
                || (radiusKm != null && userLat != null && userLon != null)
                || "closest".equalsIgnoreCase(sort) || "lowest_cost".equalsIgnoreCase(sort);

        List<JobListing> source;
        if (needsMemoryProcessing) {
            source = jobListingRepository.findAllWithFilters(jobStatus, categoryId, city);
        } else {
            Page<JobListing> page = jobListingRepository.findWithFilters(jobStatus, categoryId, city, pageable);
            List<DtoMapper.JobListingDto> pageItems = page.getContent().stream()
                    .map(j -> DtoMapper.toJobDto(j, computeDistance(j, userLat, userLon)))
                    .collect(Collectors.toList());
            return new PageImpl<>(pageItems, pageable, page.getTotalElements());
        }

        List<DtoMapper.JobListingDto> items = source.stream()
                .map(j -> DtoMapper.toJobDto(j, computeDistance(j, userLat, userLon)))
                .filter(dto -> filterCategories == null || filterCategories.contains(dto.categoryId()))
                .filter(dto -> minTokenCost == null || dto.tokenCost() >= minTokenCost)
                .filter(dto -> maxTokenCost == null || dto.tokenCost() <= maxTokenCost)
                .filter(dto -> radiusKm == null || userLat == null || userLon == null
                        || dto.distance() == null || dto.distance() <= radiusKm)
                .sorted(getComparator(sort))
                .collect(Collectors.toList());

        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), items.size());
        List<DtoMapper.JobListingDto> pageSlice = start >= items.size() ? List.of() : items.subList(start, end);
        return new PageImpl<>(pageSlice, pageable, items.size());
    }

    private Comparator<DtoMapper.JobListingDto> getComparator(String sort) {
        if ("closest".equalsIgnoreCase(sort)) {
            return Comparator.comparing(DtoMapper.JobListingDto::distance,
                    Comparator.nullsLast(Comparator.naturalOrder()));
        }
        if ("lowest_cost".equalsIgnoreCase(sort)) {
            return Comparator.comparing(DtoMapper.JobListingDto::tokenCost);
        }
        return Comparator.comparing(DtoMapper.JobListingDto::createdAt,
                Comparator.nullsLast(Comparator.reverseOrder()));
    }

    public Page<DtoMapper.JobListingDto> getMyJobs(UUID userId, Pageable pageable) {
        return jobListingRepository.findByUserId(userId, pageable)
                .map(j -> DtoMapper.toJobDto(j, null));
    }

    @Transactional
    public DtoMapper.JobListingDto updateJob(UUID userId, UUID jobId, CreateJobRequest req) {
        JobListing job = getOwnedJob(userId, jobId);
        if (!"OPEN".equals(job.getStatus())) {
            throw new IllegalArgumentException("Možete menjati samo otvorene poslove");
        }
        Category category = categoryRepository.findById(req.categoryId())
                .orElseThrow(() -> new IllegalArgumentException("Kategorija nije pronađena"));
        var aiResult = aiScoringService.scoreJob(category.getName(), req.description());
        job.setCategory(category);
        job.setTitle(req.title());
        job.setDescription(req.description());
        job.setAddress(req.address());
        job.setCity(req.city());
        job.setLatitude(req.latitude());
        job.setLongitude(req.longitude());
        job.setImages(req.images());
        job.setAiScore(aiResult.score());
        job.setTokenCost(aiResult.score() * category.getBaseTokenCost());
        jobListingRepository.save(job);
        return DtoMapper.toJobDto(job, null);
    }

    @Transactional
    public void cancelJob(UUID userId, UUID jobId) {
        JobListing job = getOwnedJob(userId, jobId);
        job.setStatus("CANCELLED");
        jobListingRepository.save(job);
    }

    @Transactional
    public DtoMapper.JobListingDto completeJob(UUID userId, String role, UUID jobId) {
        JobListing job = jobListingRepository.findById(jobId)
                .orElseThrow(() -> new IllegalArgumentException("Posao nije pronađen"));

        boolean allowed = "ROLE_CLIENT".equals(role) && job.getUserId().equals(userId)
                || "ROLE_HANDYMAN".equals(role) && userId.equals(job.getSelectedHandymanId());
        if (!allowed) {
            throw new IllegalStateException("Nemate dozvolu da završite ovaj posao");
        }
        if (!"IN_PROGRESS".equals(job.getStatus())) {
            throw new IllegalArgumentException("Posao nije u toku");
        }
        job.setStatus("COMPLETED");
        job.setCompletedAt(Instant.now());
        jobListingRepository.save(job);

        String reviewLink = frontendUrl + "/reviews/" + jobId;
        userRepository.findById(job.getUserId()).ifPresent(u ->
                emailService.send(u.getEmail(), "Posao završen",
                        "Posao je označen kao završen. Ostavite recenziju: " + reviewLink));
        if (job.getSelectedHandymanId() != null) {
            handymanRepository.findById(job.getSelectedHandymanId()).ifPresent(h ->
                    emailService.send(h.getEmail(), "Posao završen",
                            "Posao je označen kao završen. Ostavite recenziju: " + reviewLink));
        }
        return DtoMapper.toJobDto(job, null);
    }

    private JobListing getOwnedJob(UUID userId, UUID jobId) {
        JobListing job = jobListingRepository.findById(jobId)
                .orElseThrow(() -> new IllegalArgumentException("Posao nije pronađen"));
        if (!job.getUserId().equals(userId)) {
            throw new IllegalStateException("Nemate pristup ovom poslu");
        }
        return job;
    }

    private Double computeDistance(JobListing job, Double userLat, Double userLon) {
        if (userLat == null || userLon == null || job.getLatitude() == null || job.getLongitude() == null) {
            return null;
        }
        return GeoUtils.haversineKm(userLat, userLon, job.getLatitude(), job.getLongitude());
    }
}
