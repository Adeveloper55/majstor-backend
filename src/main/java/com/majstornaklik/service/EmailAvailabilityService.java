package com.majstornaklik.service;

import com.majstornaklik.dto.EmailAvailabilityResponse;
import com.majstornaklik.repository.AdminRepository;
import com.majstornaklik.repository.CompanyRegistrationRepository;
import com.majstornaklik.repository.HandymanRepository;
import com.majstornaklik.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class EmailAvailabilityService {

    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    private final UserRepository userRepository;
    private final HandymanRepository handymanRepository;
    private final AdminRepository adminRepository;
    private final CompanyRegistrationRepository companyRegistrationRepository;

    public EmailAvailabilityResponse check(String rawEmail) {
        if (rawEmail == null || rawEmail.isBlank()) {
            return new EmailAvailabilityResponse(false, "Unesite email adresu.");
        }

        String email = rawEmail.trim().toLowerCase();
        if (!EMAIL_PATTERN.matcher(email).matches()) {
            return new EmailAvailabilityResponse(false, "Unesite ispravnu email adresu.");
        }

        if (userRepository.existsByEmail(email)
                || handymanRepository.existsByEmail(email)
                || adminRepository.existsByEmail(email)) {
            return new EmailAvailabilityResponse(false, "Email je već zauzet.");
        }

        if (companyRegistrationRepository.existsByEmailAndStatus(email, "PENDING")) {
            return new EmailAvailabilityResponse(false, "Email je već zauzet (prijava preduzeća na čekanju).");
        }

        return new EmailAvailabilityResponse(true, "Email je dostupan.");
    }
}
