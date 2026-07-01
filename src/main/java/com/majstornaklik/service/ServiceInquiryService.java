package com.majstornaklik.service;

import com.majstornaklik.dto.ServiceInquiryDto;
import com.majstornaklik.dto.ServiceInquiryRequest;
import com.majstornaklik.entity.ServiceInquiry;
import com.majstornaklik.repository.ServiceInquiryRepository;
import com.majstornaklik.security.PrimaryAdminAuthorization;
import com.majstornaklik.util.PhoneUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ServiceInquiryService {

    private final ServiceInquiryRepository repository;
    private final EmailService emailService;
    private final PrimaryAdminAuthorization primaryAdminAuthorization;

    @Value("${app.admin.email:admin@majstornaklik.rs}")
    private String primaryAdminEmail;

    @Transactional
    public ServiceInquiryDto submit(ServiceInquiryRequest req) {
        String phone = PhoneUtils.normalizeOptional(req.phone());
        ServiceInquiry inquiry = ServiceInquiry.builder()
                .categorySlug(req.categorySlug().trim())
                .categoryName(req.categoryName().trim())
                .city(req.city().trim())
                .startTimeline(req.startTimeline().trim())
                .shortDescription(trimToNull(req.shortDescription()))
                .detailedDescription(req.detailedDescription().trim())
                .salutation(trimToNull(req.salutation()))
                .fullName(req.fullName().trim())
                .email(req.email().trim().toLowerCase())
                .phone(phone)
                .status("NEW")
                .build();
        repository.save(inquiry);

        emailService.send(
                primaryAdminEmail.trim(),
                "Novi upit — " + inquiry.getCategoryName() + " (" + inquiry.getCity() + ")",
                buildAdminEmailBody(inquiry));

        return ServiceInquiryDto.from(inquiry);
    }

    public Page<ServiceInquiryDto> listForPrimaryAdmin(Pageable pageable) {
        primaryAdminAuthorization.requirePrimaryAdmin();
        return repository.findAllByOrderByCreatedAtDesc(pageable).map(ServiceInquiryDto::from);
    }

    public ServiceInquiryDto getForPrimaryAdmin(UUID id) {
        primaryAdminAuthorization.requirePrimaryAdmin();
        return repository.findById(id)
                .map(ServiceInquiryDto::from)
                .orElseThrow(() -> new IllegalArgumentException("Upit nije pronađen"));
    }

    @Transactional
    public ServiceInquiryDto markRead(UUID id) {
        primaryAdminAuthorization.requirePrimaryAdmin();
        ServiceInquiry inquiry = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Upit nije pronađen"));
        inquiry.setStatus("READ");
        return ServiceInquiryDto.from(repository.save(inquiry));
    }

    public long countNew() {
        return repository.countByStatus("NEW");
    }

    private String trimToNull(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String buildAdminEmailBody(ServiceInquiry inquiry) {
        return """
                Novi upit sa sajta Majstor 365

                Kategorija: %s
                Grad: %s
                Početak radova: %s

                Ukratko: %s

                Opis:
                %s

                Kontakt:
                %s %s
                Email: %s
                Telefon: %s
                """.formatted(
                inquiry.getCategoryName(),
                inquiry.getCity(),
                inquiry.getStartTimeline(),
                inquiry.getShortDescription() != null ? inquiry.getShortDescription() : "—",
                inquiry.getDetailedDescription(),
                inquiry.getSalutation() != null ? inquiry.getSalutation() : "",
                inquiry.getFullName(),
                inquiry.getEmail(),
                inquiry.getPhone() != null ? inquiry.getPhone() : "—"
        );
    }
}
