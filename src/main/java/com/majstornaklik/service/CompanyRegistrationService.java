package com.majstornaklik.service;

import com.majstornaklik.dto.CompanyRegistrationDto;
import com.majstornaklik.dto.CompanyRegistrationSubmitResponse;
import com.majstornaklik.dto.RegisterCompanyRequest;
import com.majstornaklik.entity.CompanyRegistrationRequest;
import com.majstornaklik.entity.Handyman;
import com.majstornaklik.repository.CompanyRegistrationRepository;
import com.majstornaklik.repository.HandymanRepository;
import com.majstornaklik.repository.UserRepository;
import com.majstornaklik.util.JsonUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CompanyRegistrationService {

    private final CompanyRegistrationRepository repository;
    private final HandymanRepository handymanRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    @Transactional
    public CompanyRegistrationSubmitResponse submit(RegisterCompanyRequest req) {
        if (userRepository.existsByEmail(req.email()) || handymanRepository.existsByEmail(req.email())) {
            throw new IllegalArgumentException("Email je već registrovan");
        }
        if (repository.existsByEmailAndStatus(req.email(), "PENDING")) {
            throw new IllegalArgumentException("Već postoji prijava na čekanju za ovaj email");
        }

        CompanyRegistrationRequest entity = CompanyRegistrationRequest.builder()
                .email(req.email().trim().toLowerCase())
                .phone(req.phone().trim())
                .normalizedPhone(req.normalizedPhone().trim())
                .selectedServiceIds(JsonUtils.toJson(req.selectedServiceIds()))
                .selectedServiceNames(JsonUtils.toJson(req.selectedServiceNames()))
                .companyShortDescription(req.companyShortDescription())
                .selectedDistricts(JsonUtils.toJson(req.selectedDistricts()))
                .companyName(req.companyName().trim())
                .pib(req.pib().trim())
                .address(req.address().trim())
                .postalCode(req.postalCode().trim())
                .city(req.city().trim())
                .country(req.country().trim())
                .contactPerson(req.contactPerson().trim())
                .passwordHash(passwordEncoder.encode(req.password()))
                .status("PENDING")
                .build();

        repository.save(entity);

        emailService.send("admin@majstornaklik.rs", "Nova registracija preduzeća",
                "Preduzeće: " + entity.getCompanyName() + "\nEmail: " + entity.getEmail()
                        + "\nKontakt: " + entity.getContactPerson() + "\nTelefon: " + entity.getNormalizedPhone());

        return new CompanyRegistrationSubmitResponse(
                entity.getId().toString(),
                "PENDING",
                "Prijava je poslata. Admin će pregledati i odobriti registraciju."
        );
    }

    public Page<CompanyRegistrationDto> listForAdmin(String status, Pageable pageable) {
        if (status != null && !status.isBlank()) {
            return repository.findByStatusOrderByCreatedAtDesc(status, pageable)
                    .map(CompanyRegistrationDto::from);
        }
        return repository.findAllByOrderByCreatedAtDesc(pageable).map(CompanyRegistrationDto::from);
    }

    public CompanyRegistrationDto get(UUID id) {
        return repository.findById(id)
                .map(CompanyRegistrationDto::from)
                .orElseThrow(() -> new IllegalArgumentException("Prijava nije pronađena"));
    }

    @Transactional
    public CompanyRegistrationDto approve(UUID id) {
        CompanyRegistrationRequest req = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Prijava nije pronađena"));

        if (!"PENDING".equals(req.getStatus())) {
            throw new IllegalArgumentException("Prijava je već obrađena");
        }
        if (userRepository.existsByEmail(req.getEmail()) || handymanRepository.existsByEmail(req.getEmail())) {
            throw new IllegalArgumentException("Email je već registrovan u sistemu");
        }

        String bio = buildBio(req);

        Handyman handyman = Handyman.builder()
                .fullName(req.getCompanyName())
                .email(req.getEmail())
                .passwordHash(req.getPasswordHash())
                .phone(req.getNormalizedPhone())
                .city(req.getCity())
                .bio(bio)
                .companyName(req.getCompanyName())
                .pib(req.getPib())
                .address(req.getAddress())
                .postalCode(req.getPostalCode())
                .country(req.getCountry())
                .contactPerson(req.getContactPerson())
                .isCompany(true)
                .isVerified(true)
                .coverageDistrictsJson(req.getSelectedDistricts())
                .serviceCategoriesJson(req.getSelectedServiceNames())
                .build();

        handymanRepository.save(handyman);

        req.setStatus("APPROVED");
        req.setHandymanId(handyman.getId());
        req.setReviewedAt(Instant.now());
        repository.save(req);

        emailService.send(req.getEmail(), "Registracija preduzeća odobrena",
                "Poštovani,\n\nVaša registracija preduzeća \"" + req.getCompanyName()
                        + "\" je odobrena. Možete se prijaviti na platformu.");

        return CompanyRegistrationDto.from(req);
    }

    @Transactional
    public CompanyRegistrationDto reject(UUID id, String adminNote) {
        CompanyRegistrationRequest req = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Prijava nije pronađena"));

        if (!"PENDING".equals(req.getStatus())) {
            throw new IllegalArgumentException("Prijava je već obrađena");
        }

        req.setStatus("REJECTED");
        req.setAdminNote(adminNote);
        req.setReviewedAt(Instant.now());
        repository.save(req);

        emailService.send(req.getEmail(), "Registracija preduzeća odbijena",
                "Poštovani,\n\nVaša registracija preduzeća nije odobrena."
                        + (adminNote != null && !adminNote.isBlank() ? "\n\nNapomena: " + adminNote : ""));

        return CompanyRegistrationDto.from(req);
    }

    public long countPending() {
        return repository.countByStatus("PENDING");
    }

    private String buildBio(CompanyRegistrationRequest req) {
        StringBuilder sb = new StringBuilder();
        if (req.getCompanyShortDescription() != null && !req.getCompanyShortDescription().isBlank()) {
            sb.append(req.getCompanyShortDescription().trim());
        }
        sb.append("\n\nKontakt osoba: ").append(req.getContactPerson());
        sb.append("\nAdresa: ").append(req.getAddress()).append(", ").append(req.getPostalCode())
                .append(" ").append(req.getCity());
        return sb.toString().trim();
    }
}
