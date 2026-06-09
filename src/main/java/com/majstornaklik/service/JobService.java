package com.majstornaklik.service;

import com.majstornaklik.dto.ClientContactDto;
import com.majstornaklik.dto.HandymanContactDto;
import com.majstornaklik.dto.CreateJobRequest;
import com.majstornaklik.dto.DtoMapper;
import com.majstornaklik.dto.ScorePreviewRequest;
import com.majstornaklik.dto.ScorePreviewResponse;
import com.majstornaklik.entity.Category;
import com.majstornaklik.entity.Handyman;
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
    private final HandymanCategoryService handymanCategoryService;

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
                .tokenCost(0)
                .status("PENDING_APPROVAL")
                .build();
        jobListingRepository.save(job);
        emailService.sendToAdmin("Novi oglas na čekanju",
                "Klijent je poslao oglas \"" + job.getTitle() + "\". Odobrite u admin panelu → Poslovi na čekanju.");
        return DtoMapper.toJobDto(job, null, null, null, true);
    }

    @Transactional(readOnly = true)
    public DtoMapper.JobListingDto getJob(UUID id, Double userLat, Double userLon, Double radiusKm,
                                          UUID viewerId, String viewerRole) {
        JobListing job = jobListingRepository.findByIdWithCategory(id)
                .orElseThrow(() -> new IllegalArgumentException("Posao nije pronađen"));
        if ("PENDING_APPROVAL".equals(job.getStatus())) {
            boolean isOwner = viewerId != null && viewerId.equals(job.getUserId());
            if (!"ROLE_ADMIN".equals(viewerRole) && !isOwner) {
                throw new IllegalArgumentException("Posao nije pronađen");
            }
        }
        if ("ROLE_HANDYMAN".equals(viewerRole) && viewerId != null) {
            assertHandymanCanViewJob(job, viewerId);
        }
        Double distance = computeDistance(job, userLat, userLon);
        if (radiusKm != null && distance != null && distance > radiusKm) {
            throw new IllegalArgumentException("Posao nije u zadatom radijusu");
        }
        ClientContactDto clientContact = resolveClientContact(job, viewerId, viewerRole);
        HandymanContactDto assignedHandyman = resolveAssignedHandymanContact(job, viewerId, viewerRole);
        boolean hideTokenCost = "ROLE_CLIENT".equals(viewerRole)
                && viewerId != null && job.getUserId().equals(viewerId);
        return DtoMapper.toJobDto(job, distance, clientContact, assignedHandyman, hideTokenCost);
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

    @Transactional(readOnly = true)
    public Page<DtoMapper.JobListingDto> listAvailableJobsForHandyman(
            UUID handymanId,
            List<Integer> categoryIds,
            String city,
            Double userLat,
            Double userLon,
            Double radiusKm,
            Integer minTokenCost,
            Integer maxTokenCost,
            String sort,
            Pageable pageable) {
        Handyman handyman = handymanRepository.findById(handymanId)
                .orElseThrow(() -> new IllegalArgumentException("Majstor nije pronađen"));
        List<Integer> allowedCategories = handymanCategoryService.getCategoryIds(handyman);
        if (allowedCategories.isEmpty()) {
            return Page.empty(pageable);
        }
        final List<Integer> filterCategories = categoryIds != null && !categoryIds.isEmpty()
                ? categoryIds.stream().filter(allowedCategories::contains).toList()
                : allowedCategories;
        if (filterCategories.isEmpty()) {
            return Page.empty(pageable);
        }

        List<DtoMapper.JobListingDto> items = jobListingRepository.findAllAdminApprovedOpen().stream()
                .filter(j -> filterCategories.contains(j.getCategory().getId()))
                .filter(j -> city == null || city.isBlank()
                        || (j.getCity() != null && j.getCity().toLowerCase().contains(city.toLowerCase())))
                .map(j -> DtoMapper.toJobDto(j, computeDistance(j, userLat, userLon)))
                .filter(dto -> minTokenCost == null || dto.tokenCost() >= minTokenCost)
                .filter(dto -> maxTokenCost == null || dto.tokenCost() <= maxTokenCost)
                .filter(dto -> radiusKm == null || userLat == null || userLon == null
                        || dto.distance() == null || dto.distance() <= radiusKm)
                .sorted(getComparator(sort))
                .toList();

        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), items.size());
        List<DtoMapper.JobListingDto> pageSlice = start >= items.size() ? List.of() : items.subList(start, end);
        return new PageImpl<>(pageSlice, pageable, items.size());
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
                                                   Pageable pageable, List<Integer> restrictedCategoryIds,
                                                   boolean publishedOnly) {
        String jobStatus = status != null ? status : "OPEN";
        List<Integer> filterCategories = categoryIds != null && !categoryIds.isEmpty()
                ? categoryIds
                : (categoryId != null ? List.of(categoryId) : null);

        if (restrictedCategoryIds != null) {
            if (restrictedCategoryIds.isEmpty()) {
                return Page.empty(pageable);
            }
            if (filterCategories == null) {
                filterCategories = restrictedCategoryIds;
            } else {
                filterCategories = filterCategories.stream()
                        .filter(restrictedCategoryIds::contains)
                        .toList();
                if (filterCategories.isEmpty()) {
                    return Page.empty(pageable);
                }
            }
        }

        final List<Integer> effectiveFilterCategories = filterCategories;

        boolean needsMemoryProcessing = effectiveFilterCategories != null
                || minTokenCost != null || maxTokenCost != null
                || (radiusKm != null && userLat != null && userLon != null)
                || "closest".equalsIgnoreCase(sort) || "lowest_cost".equalsIgnoreCase(sort);

        List<JobListing> source;
        if (needsMemoryProcessing) {
            source = jobListingRepository.findAllWithFilters(jobStatus, categoryId, city);
        } else {
            Page<JobListing> page = jobListingRepository.findWithFilters(jobStatus, categoryId, city, pageable);
            List<DtoMapper.JobListingDto> pageItems = page.getContent().stream()
                    .filter(j -> !publishedOnly || isPublishedForHandymen(j))
                    .map(j -> DtoMapper.toJobDto(j, computeDistance(j, userLat, userLon)))
                    .collect(Collectors.toList());
            return new PageImpl<>(pageItems, pageable, pageItems.size());
        }

        List<DtoMapper.JobListingDto> items = source.stream()
                .filter(j -> !publishedOnly || isPublishedForHandymen(j))
                .map(j -> DtoMapper.toJobDto(j, computeDistance(j, userLat, userLon)))
                .filter(dto -> effectiveFilterCategories == null || effectiveFilterCategories.contains(dto.categoryId()))
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

    @Transactional(readOnly = true)
    public Page<DtoMapper.JobListingDto> getMyJobs(UUID userId, Pageable pageable) {
        return jobListingRepository.findByUserIdWithCategory(userId, pageable)
                .map(j -> toClientJobDto(j, userId));
    }

    @Transactional
    public DtoMapper.JobListingDto updateJob(UUID userId, UUID jobId, CreateJobRequest req) {
        JobListing job = getOwnedJob(userId, jobId);
        if (!"OPEN".equals(job.getStatus()) && !"PENDING_APPROVAL".equals(job.getStatus())) {
            throw new IllegalArgumentException("Možete menjati samo poslove na čekanju ili otvorene poslove");
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
        jobListingRepository.save(job);
        return DtoMapper.toJobDto(job, null, null, null, true);
    }

    @Transactional
    public void cancelJob(UUID userId, UUID jobId) {
        JobListing job = getOwnedJob(userId, jobId);
        if (job.getSelectedHandymanId() != null) {
            throw new IllegalArgumentException("Ne možete obrisati posao kojem je dodeljen majstor");
        }
        if ("IN_PROGRESS".equals(job.getStatus()) || "COMPLETED".equals(job.getStatus())) {
            throw new IllegalArgumentException("Ne možete obrisati posao koji je u toku ili završen");
        }
        if ("CANCELLED".equals(job.getStatus())) {
            throw new IllegalArgumentException("Posao je već otkazan");
        }
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
        if ("ROLE_CLIENT".equals(role)) {
            return toClientJobDto(job, userId);
        }
        ClientContactDto contact = userRepository.findById(job.getUserId())
                .map(DtoMapper::toClientContact).orElse(null);
        return DtoMapper.toJobDto(job, null, contact);
    }

    private DtoMapper.JobListingDto toClientJobDto(JobListing job, UUID clientId) {
        HandymanContactDto assigned = resolveAssignedHandymanContact(job, clientId, "ROLE_CLIENT");
        return DtoMapper.toJobDto(job, null, null, assigned, true);
    }

    private HandymanContactDto resolveAssignedHandymanContact(JobListing job, UUID viewerId, String viewerRole) {
        if (viewerId == null || !"ROLE_CLIENT".equals(viewerRole)) {
            return null;
        }
        if (!job.getUserId().equals(viewerId) || job.getSelectedHandymanId() == null) {
            return null;
        }
        if (!"IN_PROGRESS".equals(job.getStatus()) && !"COMPLETED".equals(job.getStatus())) {
            return null;
        }
        return handymanRepository.findById(job.getSelectedHandymanId())
                .map(DtoMapper::toHandymanContact)
                .orElse(null);
    }

    private boolean isPublishedForHandymen(JobListing job) {
        return "OPEN".equals(job.getStatus())
                && job.getTokenCost() != null
                && job.getTokenCost() > 0;
    }

    private void assertHandymanCanViewJob(JobListing job, UUID handymanId) {
        if ("OPEN".equals(job.getStatus())) {
            if (!isPublishedForHandymen(job)) {
                throw new IllegalArgumentException("Posao nije pronađen");
            }
            Handyman handyman = handymanRepository.findById(handymanId)
                    .orElseThrow(() -> new IllegalArgumentException("Majstor nije pronađen"));
            handymanCategoryService.assertCanAccessJobCategory(handyman, job.getCategory().getId());
            return;
        }
        if (("IN_PROGRESS".equals(job.getStatus()) || "COMPLETED".equals(job.getStatus()))
                && handymanId.equals(job.getSelectedHandymanId())) {
            return;
        }
        throw new IllegalArgumentException("Posao nije pronađen");
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
