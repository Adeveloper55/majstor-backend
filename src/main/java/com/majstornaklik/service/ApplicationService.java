package com.majstornaklik.service;

import com.majstornaklik.dto.ClientContactDto;
import com.majstornaklik.dto.DtoMapper;
import com.majstornaklik.entity.*;
import com.majstornaklik.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ApplicationService {

    private final JobApplicationRepository applicationRepository;
    private final JobListingRepository jobListingRepository;
    private final HandymanRepository handymanRepository;
    private final UserRepository userRepository;
    private final TokenTransactionRepository tokenTransactionRepository;
    private final HandymanCategoryService handymanCategoryService;

    @Transactional
    public Map<String, Object> unlock(UUID handymanId, UUID jobId) {
        JobListing job = jobListingRepository.findByIdWithCategory(jobId)
                .orElseThrow(() -> new IllegalArgumentException("Posao nije pronađen"));
        if (!"OPEN".equals(job.getStatus())) {
            throw new IllegalArgumentException("Posao nije dostupan");
        }
        if (job.getTokenCost() == null || job.getTokenCost() <= 0) {
            throw new IllegalArgumentException("Posao nije dostupan za otključavanje");
        }

        Handyman handyman = handymanRepository.findById(handymanId)
                .orElseThrow(() -> new IllegalArgumentException("Majstor nije pronađen"));
        handymanCategoryService.assertCanAccessJobCategory(handyman, job.getCategory().getId());

        var existing = applicationRepository.findByJobListingIdAndHandymanId(jobId, handymanId);
        if (existing.isPresent()) {
            JobApplication app = existing.get();
            if (isUnlockedStatus(app.getStatus())) {
                return buildUnlockResponse(app, job);
            }
            throw new IllegalArgumentException("Već imate zabeležen zahtev za ovaj posao");
        }

        if (handyman.getTokenBalance() < job.getTokenCost()) {
            throw new IllegalArgumentException("Nemate dovoljno tokena. Pošaljite zahtev za dopunu u sekciji Tokeni.");
        }

        handyman.setTokenBalance(handyman.getTokenBalance() - job.getTokenCost());
        handymanRepository.save(handyman);

        JobApplication application = JobApplication.builder()
                .jobListingId(jobId)
                .handyman(handyman)
                .tokensSpent(job.getTokenCost())
                .status("UNLOCKED")
                .build();
        applicationRepository.save(application);

        tokenTransactionRepository.save(TokenTransaction.builder()
                .handymanId(handymanId)
                .jobApplicationId(application.getId())
                .amount(-job.getTokenCost())
                .type("DEDUCTED")
                .description("Otključan kontakt: " + job.getTitle())
                .build());

        return buildUnlockResponse(application, job);
    }

    @Transactional(readOnly = true)
    public Page<Map<String, Object>> listForJob(UUID clientId, UUID jobId, Pageable pageable) {
        JobListing job = jobListingRepository.findById(jobId)
                .orElseThrow(() -> new IllegalArgumentException("Posao nije pronađen"));
        if (!job.getUserId().equals(clientId)) {
            throw new IllegalStateException("Nemate pristup prijavama");
        }
        return applicationRepository.findByJobListingId(jobId, pageable).map(this::toDto);
    }

    @Transactional(readOnly = true)
    public Page<Map<String, Object>> listForHandyman(UUID handymanId, Pageable pageable) {
        return applicationRepository.findUnlockedByHandymanId(handymanId, pageable)
                .map(this::toHandymanApplicationDto);
    }

    @Transactional(readOnly = true)
    public List<DtoMapper.JobListingDto> listUnlockedJobsForHandyman(UUID handymanId) {
        return applicationRepository.findUnlockedByHandymanId(handymanId).stream()
                .map(app -> jobListingRepository.findByIdWithCategory(app.getJobListingId()))
                .flatMap(java.util.Optional::stream)
                .map(j -> {
                    ClientContactDto contact = userRepository.findById(j.getUserId())
                            .map(DtoMapper::toClientContact)
                            .orElse(null);
                    return DtoMapper.toJobDto(j, null, contact, null, false, true, true);
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public Page<Map<String, Object>> listPendingForAdmin(Pageable pageable) {
        return applicationRepository.findByStatusOrderByAppliedAtDesc("PENDING", pageable)
                .map(this::toAdminApplicationDto);
    }

    @Transactional
    public void assignHandymanByAdmin(UUID jobId, UUID handymanId) {
        throw new UnsupportedOperationException("Dodela majstora više nije podržana — majstori kupuju kontakt direktno.");
    }

    @Transactional
    public void assignByApplicationId(UUID applicationId) {
        throw new UnsupportedOperationException("Dodela majstora više nije podržana — majstori kupuju kontakt direktno.");
    }

    @Transactional(readOnly = true)
    public Page<Map<String, Object>> listForJobAdmin(UUID jobId, Pageable pageable) {
        jobListingRepository.findById(jobId)
                .orElseThrow(() -> new IllegalArgumentException("Posao nije pronađen"));
        return applicationRepository.findByJobListingId(jobId, pageable).map(this::toDto);
    }

    public boolean hasUnlocked(UUID handymanId, UUID jobId) {
        return applicationRepository.findByJobListingIdAndHandymanId(jobId, handymanId)
                .map(a -> isUnlockedStatus(a.getStatus()))
                .orElse(false);
    }

    public List<UUID> findUnlockedJobIds(UUID handymanId) {
        return applicationRepository.findUnlockedByHandymanId(handymanId).stream()
                .map(JobApplication::getJobListingId)
                .toList();
    }

    public List<Map<String, Object>> listRecentForClient(UUID clientId) {
        return jobListingRepository.findByUserId(clientId, PageRequest.of(0, 50)).getContent().stream()
                .flatMap(job -> applicationRepository.findByJobListingId(job.getId()).stream()
                        .filter(a -> isUnlockedStatus(a.getStatus()))
                        .map(app -> {
                            Map<String, Object> dto = new java.util.HashMap<>(toDto(app));
                            dto.put("jobTitle", job.getTitle());
                            dto.put("jobId", job.getId().toString());
                            return dto;
                        }))
                .sorted((a, b) -> ((java.time.Instant) b.get("appliedAt")).compareTo((java.time.Instant) a.get("appliedAt")))
                .limit(10)
                .toList();
    }

    private Map<String, Object> buildUnlockResponse(JobApplication application, JobListing job) {
        ClientContactDto contact = userRepository.findById(job.getUserId())
                .map(DtoMapper::toClientContact)
                .orElse(null);
        Map<String, Object> result = new java.util.HashMap<>();
        result.put("id", application.getId());
        result.put("status", application.getStatus());
        result.put("tokensSpent", application.getTokensSpent());
        result.put("city", job.getCity());
        result.put("clientContact", contact);
        return result;
    }

    private boolean isUnlockedStatus(String status) {
        return "UNLOCKED".equals(status) || "ACCEPTED".equals(status);
    }

    private Map<String, Object> toAdminApplicationDto(JobApplication app) {
        Map<String, Object> dto = new java.util.HashMap<>();
        dto.put("id", app.getId());
        dto.put("jobListingId", app.getJobListingId());
        dto.put("status", app.getStatus());
        dto.put("coverMessage", app.getCoverMessage() != null ? app.getCoverMessage() : "");
        dto.put("appliedAt", app.getAppliedAt());
        Handyman h = app.getHandyman();
        dto.put("handyman", DtoMapper.toHandymanPublicDto(h));
        dto.put("handymanEmail", h.getEmail());
        dto.put("handymanTokenBalance", h.getTokenBalance());
        jobListingRepository.findByIdWithCategory(app.getJobListingId()).ifPresent(j -> {
            dto.put("jobTitle", j.getTitle());
            dto.put("jobCity", j.getCity());
            dto.put("jobCategory", j.getCategory().getName());
            dto.put("jobTokenCost", j.getTokenCost());
            dto.put("jobStatus", j.getStatus());
            userRepository.findById(j.getUserId()).ifPresent(u -> {
                dto.put("clientName", u.getFullName());
                dto.put("clientEmail", u.getEmail());
            });
        });
        return dto;
    }

    private Map<String, Object> toHandymanApplicationDto(JobApplication app) {
        Map<String, Object> dto = new java.util.HashMap<>();
        dto.put("id", app.getId());
        dto.put("jobListingId", app.getJobListingId());
        dto.put("tokensSpent", app.getTokensSpent());
        dto.put("coverMessage", app.getCoverMessage() != null ? app.getCoverMessage() : "");
        dto.put("status", app.getStatus());
        dto.put("appliedAt", app.getAppliedAt());
        jobListingRepository.findByIdWithCategory(app.getJobListingId()).ifPresent(j -> {
            dto.put("jobTitle", j.getTitle());
            dto.put("jobCity", j.getCity());
            dto.put("jobStatus", j.getStatus());
            dto.put("jobTokenCost", j.getTokenCost());
            dto.put("jobCategory", j.getCategory().getName());
        });
        return dto;
    }

    private Map<String, Object> toDto(JobApplication app) {
        Map<String, Object> dto = new java.util.HashMap<>();
        dto.put("id", app.getId());
        dto.put("jobListingId", app.getJobListingId());
        dto.put("handyman", DtoMapper.toHandymanPublicDto(app.getHandyman()));
        dto.put("tokensSpent", app.getTokensSpent());
        dto.put("coverMessage", app.getCoverMessage() != null ? app.getCoverMessage() : "");
        dto.put("status", app.getStatus());
        dto.put("appliedAt", app.getAppliedAt());
        return dto;
    }
}
