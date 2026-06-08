package com.majstornaklik.service;

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

import java.math.BigDecimal;
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
        emailService.send(handyman.getEmail(), "Dobrodošli na Majstor na klik",
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
                .latitude(req.latitude())
                .longitude(req.longitude())
                .images(req.images())
                .aiScore(null)
                .tokenCost(req.tokenCost())
                .status("OPEN")
                .build();
        jobListingRepository.save(job);
        return DtoMapper.toJobDto(job, null);
    }

    @Transactional(readOnly = true)
    public Page<DtoMapper.JobListingDto> listJobs(Pageable pageable) {
        return jobListingRepository.findAll(pageable).map(j -> DtoMapper.toJobDto(j, null));
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

    public Page<Map<String, Object>> listTokenRequests(String status, Pageable pageable) {
        Page<TokenPurchaseRequest> page = status != null && !status.isBlank()
                ? requestRepository.findByStatus(status, pageable)
                : requestRepository.findAllByOrderByCreatedAtDesc(pageable);
        return page.map(r -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id", r.getId());
            m.put("handyman", DtoMapper.toHandymanPublicDto(r.getHandyman()));
            m.put("handymanEmail", r.getHandyman().getEmail());
            m.put("tokenAmount", r.getTokenAmount());
            m.put("amountExpected", r.getAmountExpected());
            m.put("paymentReference", r.getPaymentReference());
            m.put("status", r.getStatus());
            m.put("adminNote", r.getAdminNote());
            m.put("createdAt", r.getCreatedAt());
            return m;
        });
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
        stats.put("pendingTokenRequests", requestRepository.findByStatus("PENDING", Pageable.unpaged()).getTotalElements());
        stats.put("pendingJobApplications", jobApplicationRepository.countByStatus("PENDING"));
        stats.put("totalReviews", reviewRepository.count());
        stats.put("newContactMessages", contactMessageRepository.countByStatus("NEW"));
        stats.put("pendingCompanyRegistrations", companyRegistrationRepository.countByStatus("PENDING"));

        BigDecimal revenue = requestRepository.findByStatus("APPROVED", Pageable.unpaged()).getContent().stream()
                .map(TokenPurchaseRequest::getAmountExpected)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        stats.put("totalRevenue", revenue);
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
}
