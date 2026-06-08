package com.majstornaklik.service;

import com.majstornaklik.repository.CompanyRegistrationRepository;
import com.majstornaklik.repository.HandymanRepository;
import com.majstornaklik.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PhoneUniquenessService {

    private final UserRepository userRepository;
    private final HandymanRepository handymanRepository;
    private final CompanyRegistrationRepository companyRegistrationRepository;

    public void assertPhoneAvailable(String phoneNormalized, UUID excludeUserId, UUID excludeHandymanId,
                                     UUID excludeCompanyRegistrationId) {
        if (phoneNormalized == null || phoneNormalized.isBlank()) {
            return;
        }

        boolean userTaken = excludeUserId != null
                ? userRepository.existsByPhoneNormalizedAndIdNot(phoneNormalized, excludeUserId)
                : userRepository.existsByPhoneNormalized(phoneNormalized);
        if (userTaken) {
            throw new IllegalArgumentException("Broj telefona je već registrovan.");
        }

        boolean handymanTaken = excludeHandymanId != null
                ? handymanRepository.existsByPhoneNormalizedAndIdNot(phoneNormalized, excludeHandymanId)
                : handymanRepository.existsByPhoneNormalized(phoneNormalized);
        if (handymanTaken) {
            throw new IllegalArgumentException("Broj telefona je već registrovan.");
        }

        boolean companyPendingTaken = excludeCompanyRegistrationId != null
                ? companyRegistrationRepository.existsByNormalizedPhoneAndStatusAndIdNot(
                phoneNormalized, "PENDING", excludeCompanyRegistrationId)
                : companyRegistrationRepository.existsByNormalizedPhoneAndStatus(phoneNormalized, "PENDING");
        if (companyPendingTaken) {
            throw new IllegalArgumentException("Broj telefona je već registrovan.");
        }
    }
}
