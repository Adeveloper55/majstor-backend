package com.majstornaklik.service;

import com.majstornaklik.dto.ApplyJobRequest;
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
    private final EmailService emailService;
    private final HandymanCategoryService handymanCategoryService;

    @Transactional
    public Map<String, Object> apply(UUID handymanId, UUID jobId, ApplyJobRequest req) {
        JobListing job = jobListingRepository.findById(jobId)
                .orElseThrow(() -> new IllegalArgumentException("Posao nije pronađen"));
        if (!"OPEN".equals(job.getStatus())) {
            throw new IllegalArgumentException("Posao nije otvoren za prijave");
        }
        if (applicationRepository.existsByJobListingIdAndHandymanId(jobId, handymanId)) {
            throw new IllegalArgumentException("Već ste se prijavili na ovaj posao");
        }

        Handyman handyman = handymanRepository.findById(handymanId)
                .orElseThrow(() -> new IllegalArgumentException("Majstor nije pronađen"));
        handymanCategoryService.assertCanAccessJobCategory(handyman, job.getCategory().getId());
        if (handyman.getTokenBalance() < job.getTokenCost()) {
            throw new IllegalArgumentException("Nemate dovoljno tokena za ovaj posao. Pošaljite zahtev za dopunu.");
        }

        JobApplication application = JobApplication.builder()
                .jobListingId(jobId)
                .handyman(handyman)
                .tokensSpent(0)
                .coverMessage(req != null ? req.coverMessage() : null)
                .status("PENDING")
                .build();
        applicationRepository.save(application);

        userRepository.findById(job.getUserId()).ifPresent(client ->
                emailService.send(client.getEmail(), "Nova prijava na posao",
                        "Majstor " + handyman.getFullName() + " se prijavio na vaš oglas. Admin će odobriti dodelu."));

        emailService.send("admin@majstornaklik.rs", "Novi zahtev za posao",
                "Majstor " + handyman.getFullName() + " traži posao: " + job.getTitle()
                        + " (" + job.getTokenCost() + " tokena). Pregledajte u admin panelu → Zahtevi za posao.");

        return Map.of("id", application.getId(), "status", application.getStatus());
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
        return applicationRepository.findByHandymanIdWithHandyman(handymanId, pageable)
                .map(this::toHandymanApplicationDto);
    }

    @Transactional(readOnly = true)
    public Page<Map<String, Object>> listPendingForAdmin(Pageable pageable) {
        return applicationRepository.findByStatusOrderByAppliedAtDesc("PENDING", pageable)
                .map(this::toAdminApplicationDto);
    }

    @Transactional
    public void assignHandymanByAdmin(UUID jobId, UUID handymanId) {
        JobListing job = jobListingRepository.findById(jobId)
                .orElseThrow(() -> new IllegalArgumentException("Posao nije pronađen"));
        assignHandymanToJob(job, handymanId);
    }

    @Transactional
    public void assignByApplicationId(UUID applicationId) {
        JobApplication app = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new IllegalArgumentException("Zahtev nije pronađen"));
        if (!"PENDING".equals(app.getStatus())) {
            throw new IllegalArgumentException("Zahtev je već obrađen");
        }
        JobListing job = jobListingRepository.findById(app.getJobListingId())
                .orElseThrow(() -> new IllegalArgumentException("Posao nije pronađen"));
        assignHandymanToJob(job, app.getHandyman().getId());
    }

    @Transactional(readOnly = true)
    public Page<Map<String, Object>> listForJobAdmin(UUID jobId, Pageable pageable) {
        jobListingRepository.findById(jobId)
                .orElseThrow(() -> new IllegalArgumentException("Posao nije pronađen"));
        return applicationRepository.findByJobListingId(jobId, pageable).map(this::toDto);
    }

    private void assignHandymanToJob(JobListing job, UUID handymanId) {
        if (!"OPEN".equals(job.getStatus())) {
            throw new IllegalArgumentException("Posao nije otvoren za dodelu");
        }

        Handyman handyman = handymanRepository.findById(handymanId)
                .orElseThrow(() -> new IllegalArgumentException("Majstor nije pronađen"));

        JobApplication selected = applicationRepository
                .findByJobListingIdAndHandymanId(job.getId(), handymanId)
                .orElseThrow(() -> new IllegalArgumentException("Majstor se mora prvo prijaviti na posao"));

        if (!"PENDING".equals(selected.getStatus())) {
            throw new IllegalArgumentException("Zahtev majstora je već obrađen");
        }

        if (handyman.getTokenBalance() < job.getTokenCost()) {
            throw new IllegalArgumentException("Majstor nema dovoljno tokena (" + handyman.getTokenBalance()
                    + "). Potrebno: " + job.getTokenCost());
        }

        handyman.setTokenBalance(handyman.getTokenBalance() - job.getTokenCost());
        handymanRepository.save(handyman);

        selected.setStatus("ACCEPTED");
        selected.setTokensSpent(job.getTokenCost());
        applicationRepository.save(selected);

        tokenTransactionRepository.save(TokenTransaction.builder()
                .handymanId(handymanId)
                .jobApplicationId(selected.getId())
                .amount(-job.getTokenCost())
                .type("DEDUCTED")
                .description("Odobren posao: " + job.getTitle())
                .build());

        job.setStatus("IN_PROGRESS");
        job.setSelectedHandymanId(handymanId);
        jobListingRepository.save(job);

        applicationRepository.findByJobListingId(job.getId()).stream()
                .filter(a -> !a.getHandyman().getId().equals(handymanId))
                .forEach(a -> {
                    a.setStatus("REJECTED");
                    applicationRepository.save(a);
                });

        userRepository.findById(job.getUserId()).ifPresent(client -> {
            emailService.send(handyman.getEmail(), "Dodeljen vam je posao",
                    "Posao: " + job.getTitle() + "\nSkinuto tokena: " + job.getTokenCost()
                            + "\nKontakt klijenta: " + client.getFullName()
                            + " | " + client.getEmail() + " | " + (client.getPhone() != null ? client.getPhone() : "—")
                            + "\nAdresa: " + (job.getAddress() != null ? job.getAddress() : "—") + ", " + (job.getCity() != null ? job.getCity() : "—"));
            emailService.send(client.getEmail(), "Majstor dodeljen na posao",
                    "Majstor " + handyman.getFullName() + " je dodeljen na vaš posao.\nKontakt: "
                            + handyman.getEmail() + " / " + (handyman.getPhone() != null ? handyman.getPhone() : "—"));
        });
    }

    public List<Map<String, Object>> listRecentForClient(UUID clientId) {
        return jobListingRepository.findByUserId(clientId, PageRequest.of(0, 50)).getContent().stream()
                .flatMap(job -> applicationRepository.findByJobListingId(job.getId()).stream()
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
