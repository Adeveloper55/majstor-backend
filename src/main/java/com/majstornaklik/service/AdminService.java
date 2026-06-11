package com.majstornaklik.service;

import com.majstornaklik.dto.AdminApproveJobRequest;
import com.majstornaklik.dto.AdminCreateHandymanRequest;
import com.majstornaklik.dto.AdminCreateJobRequest;
import com.majstornaklik.dto.ClientContactDto;
import com.majstornaklik.dto.DtoMapper;
import com.majstornaklik.entity.*;
import com.majstornaklik.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.majstornaklik.util.PibUtils;
import com.majstornaklik.util.PhoneUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final UserRepository userRepository;
    private final HandymanRepository handymanRepository;
    private final JobListingRepository jobListingRepository;
    private final TokenPurchaseRequestRepository requestRepository;
    private final ReviewRepository reviewRepository;
    private final TokenService tokenService;
    private final ReviewService reviewService;
    private final CategoryRepository categoryRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final ContactMessageRepository contactMessageRepository;
    private final JobApplicationRepository jobApplicationRepository;
    private final CompanyRegistrationRepository companyRegistrationRepository;
    private final PhoneUniquenessService phoneUniquenessService;

    public Page<DtoMapper.UserDto> listUsers(String search, Pageable pageable) {
        if (search != null && !search.isBlank()) {
            return userRepository.findByFullNameContainingIgnoreCaseOrEmailContainingIgnoreCase(search, search, pageable)
                    .map(DtoMapper::toUserDto);
        }
        return userRepository.findAll(pageable)
                .map(DtoMapper::toUserDto);
    }

    public DtoMapper.UserDto getUser(UUID id) {
        User user = userRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Korisnik nije pronađen"));
        return DtoMapper.toUserDto(user);
    }

    @Transactional
    public void deactivateUser(UUID id) {
        User user = userRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Korisnik nije pronađen"));
        user.setIsActive(false);
        userRepository.save(user);
    }

    public Page<DtoMapper.HandymanDto> listHandymen(String search, Pageable pageable) {
        if (search != null && !search.isBlank()) {
            return handymanRepository.findByFullNameContainingIgnoreCaseOrEmailContainingIgnoreCase(search, search, pageable)
                    .map(DtoMapper::toHandymanDto);
        }
            return handymanRepository.findAll(pageable)
                    .map(DtoMapper::toHandymanDto);
    }

    public DtoMapper.HandymanDto getHandyman(UUID id) {
        Handyman h = handymanRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Majstor nije pronađen"));
        return DtoMapper.toHandymanDto(h);
    }

    @Transactional
    public void deactivateHandyman(UUID id) {
        Handyman h = handymanRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Majstor nije pronađen"));
        h.setIsActive(false);
        handymanRepository.save(h);
    }

    @Transactional
    public DtoMapper.HandymanDto createHandyman(AdminCreateHandymanRequest req) {
        if (userRepository.existsByEmail(req.email()) || handymanRepository.existsByEmail(req.email())) {
            throw new IllegalArgumentException("Email je već registrovan");
        }
        String pib = PibUtils.normalizeOptional(req.pib());
        if (pib != null && handymanRepository.existsByPib(pib)) {
            throw new IllegalArgumentException("PIB je već registrovan");
        }
        String phoneNormalized = PhoneUtils.normalizeOptional(req.phone());
        phoneUniquenessService.assertPhoneAvailable(phoneNormalized, null, null, null);
        Handyman handyman = Handyman.builder()
                .fullName(req.fullName())
                .email(req.email())
                .passwordHash(passwordEncoder.encode(req.password()))
                .phone(phoneNormalized)
                .phoneNormalized(phoneNormalized)
                .city(req.city())
                .bio(req.bio())
                .pib(pib)
                .emailVerified(true)
                .build();
        handymanRepository.save(handyman);
        if (req.initialTokens() != null && req.initialTokens() > 0) {
            tokenService.adjustTokens(handyman.getId(), req.initialTokens(), "Početni tokeni (admin)");
        }
        emailService.sendSafely(handyman.getEmail(), "Dobrodošli na Majstor na klik",
                "Admin je kreirao vaš nalog majstora. Prijavite se sa emailom i lozinkom koju vam je admin dao.");
        return DtoMapper.toHandymanDto(handymanRepository.findById(handyman.getId()).orElseThrow());
    }

    @Transactional
    public DtoMapper.JobListingDto createJob(AdminCreateJobRequest req) {
        User client = userRepository.findById(req.clientId())
                .orElseThrow(() -> new IllegalArgumentException("Klijent nije pronađen"));
        if (!Boolean.TRUE.equals(client.getIsActive())) {
            throw new IllegalArgumentException("Klijent nije aktivan");
        }
        Category category = categoryRepository.findById(req.categoryId())
                .orElseThrow(() -> new IllegalArgumentException("Kategorija nije pronađena"));

        JobListing job = JobListing.builder()
                .userId(client.getId())
                .category(category)
                .title(req.title())
                .description(req.description())
                .address(req.address())
                .city(req.city())
                .images(req.images())
                .aiScore(null)
                .tokenCost(req.tokenCost())
                .status("OPEN")
                .build();
        boolean pinned = req.latitude() != null && req.longitude() != null;
        job.setLocationPinned(pinned);
        job.setLatitude(pinned ? req.latitude() : null);
        job.setLongitude(pinned ? req.longitude() : null);
        jobListingRepository.save(job);
        return DtoMapper.toJobDto(job, null);
    }

    @Transactional(readOnly = true)
    public Page<DtoMapper.JobListingDto> listJobs(String status, Pageable pageable) {
        Page<JobListing> page = status != null && !status.isBlank()
                ? jobListingRepository.findByStatus(status, pageable)
                : jobListingRepository.findAll(pageable);
        return page.map(j -> {
            ClientContactDto contact = userRepository.findById(j.getUserId())
                    .map(DtoMapper::toClientContact).orElse(null);
            return DtoMapper.toJobDto(j, null, contact);
        });
    }

    @Transactional
    public DtoMapper.JobListingDto approveJob(UUID id, AdminApproveJobRequest req) {
        JobListing job = jobListingRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Posao nije pronađen"));
        if (!"PENDING_APPROVAL".equals(job.getStatus())) {
            throw new IllegalArgumentException("Posao nije na čekanju odobrenja");
        }
        job.setTokenCost(req.tokenCost());
        job.setStatus("OPEN");
        jobListingRepository.save(job);
        userRepository.findById(job.getUserId()).ifPresent(u ->
                emailService.send(u.getEmail(), "Oglas odobren — sada je vidljiv",
                        "Admin je odobrio vaš oglas \"" + job.getTitle() + "\" sa cenom od "
                                + req.tokenCost() + " tokena. Oglas je sada vidljiv majstorima i izvođačima i mogu da kupe kontakt."));
        return getJob(id);
    }

    @Transactional(readOnly = true)
    public DtoMapper.JobListingDto getJob(UUID id) {
        JobListing job = jobListingRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Posao nije pronađen"));
        ClientContactDto contact = userRepository.findById(job.getUserId()).map(DtoMapper::toClientContact).orElse(null);
        return DtoMapper.toJobDto(job, null, contact);
    }

    @Transactional
    public void deleteJob(UUID id) {
        jobListingRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public Page<Map<String, Object>> listTokenRequests(String status, Pageable pageable) {
        Page<TokenPurchaseRequest> page = status != null && !status.isBlank()
                ? requestRepository.findByStatusWithHandyman(status, pageable)
                : requestRepository.findAllWithHandyman(pageable);
        return page.map(this::toTokenRequestAdminDto);
    }

    private Map<String, Object> toTokenRequestAdminDto(TokenPurchaseRequest r) {
        Handyman handyman = r.getHandyman();
        Map<String, Object> m = new HashMap<>();
        m.put("id", r.getId());
        m.put("handyman", DtoMapper.toHandymanPublicDto(handyman));
        m.put("handymanEmail", handyman.getEmail());
        m.put("tokenAmount", r.getTokenAmount());
        m.put("amountExpected", r.getAmountExpected());
        m.put("paymentReference", r.getPaymentReference() != null ? r.getPaymentReference() : "");
        m.put("status", r.getStatus());
        m.put("adminNote", r.getAdminNote());
        m.put("createdAt", r.getCreatedAt());
        m.put("predracunSentAt", r.getPredracunSentAt());
        return m;
    }

    public Page<Review> listReviews(Pageable pageable) {
        return reviewRepository.findAll(pageable);
    }

    @Transactional
    public void deleteReview(UUID id) {
        reviewService.deleteReview(id);
    }

    public Map<String, Object> getStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalUsers", userRepository.count());
        stats.put("totalHandymen", handymanRepository.count());
        stats.put("totalJobs", jobListingRepository.count());
        stats.put("openJobs", jobListingRepository.findByStatus("OPEN", Pageable.unpaged()).getTotalElements());
        stats.put("pendingJobs", jobListingRepository.findByStatus("PENDING_APPROVAL", Pageable.unpaged()).getTotalElements());
        stats.put("pendingTokenRequests", requestRepository.findByStatus("PENDING", Pageable.unpaged()).getTotalElements());
        stats.put("unlockedLeads", jobApplicationRepository.countByStatus("UNLOCKED")
                + jobApplicationRepository.countByStatus("ACCEPTED"));
        stats.put("totalReviews", reviewRepository.count());
        stats.put("newContactMessages", contactMessageRepository.countByStatus("NEW"));
        stats.put("pendingCompanyRegistrations", companyRegistrationRepository.countByStatus("PENDING"));
        return stats;
    }

    public void adjustTokens(UUID handymanId, int amount, String description) {
        tokenService.adjustTokens(handymanId, amount, description);
    }

    public void approveTokenRequest(UUID id) {
        tokenService.approveRequest(id);
    }

    public void rejectTokenRequest(UUID id, String note) {
        tokenService.rejectRequest(id, note);
    }

    public void sendTokenPredracun(UUID id) {
        tokenService.sendPredracun(id);
    }
}
