package com.majstornaklik.service;

import com.majstornaklik.dto.TokenRequestDto;
import com.majstornaklik.entity.Handyman;
import com.majstornaklik.entity.TokenPackage;
import com.majstornaklik.entity.TokenPurchaseRequest;
import com.majstornaklik.entity.TokenTransaction;
import com.majstornaklik.repository.HandymanRepository;
import com.majstornaklik.repository.TokenPackageRepository;
import com.majstornaklik.repository.TokenPurchaseRequestRepository;
import com.majstornaklik.repository.TokenTransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TokenService {

    private final TokenPackageRepository packageRepository;
    private final TokenPurchaseRequestRepository requestRepository;
    private final TokenTransactionRepository transactionRepository;
    private final HandymanRepository handymanRepository;
    private final EmailService emailService;

    @Value("${app.company.name}")
    private String companyName;

    @Value("${app.company.pib:}")
    private String companyPib;

    @Value("${app.company.bank-name}")
    private String bankName;

    @Value("${app.company.bank-account}")
    private String bankAccount;

    @Value("${app.company.payment-instructions}")
    private String paymentInstructions;

    public Page<TokenPackage> listPackages(Pageable pageable) {
        return packageRepository.findByIsActiveTrue(pageable);
    }

    public Map<String, String> getBankDetails() {
        return Map.of(
                "companyName", companyName,
                "companyPib", companyPib,
                "bankName", bankName,
                "bankAccount", bankAccount,
                "paymentInstructions", paymentInstructions
        );
    }

    @Transactional
    public Map<String, Object> submitRequest(UUID handymanId, TokenRequestDto dto) {
        Handyman handyman = handymanRepository.findById(handymanId)
                .orElseThrow(() -> new IllegalArgumentException("Majstor nije pronađen"));
        TokenPackage pkg = packageRepository.findById(dto.packageId())
                .orElseThrow(() -> new IllegalArgumentException("Paket nije pronađen"));
        if (!Boolean.TRUE.equals(pkg.getIsActive())) {
            throw new IllegalArgumentException("Paket nije aktivan");
        }

        TokenPurchaseRequest request = TokenPurchaseRequest.builder()
                .handyman(handyman)
                .tokenPackage(pkg)
                .tokenAmount(pkg.getTokenAmount())
                .amountExpected(pkg.getPriceEur())
                .paymentReference(dto.paymentReference())
                .status("PENDING")
                .build();
        requestRepository.save(request);

        return Map.of(
                "id", request.getId(),
                "status", request.getStatus(),
                "tokenAmount", request.getTokenAmount(),
                "amountExpected", request.getAmountExpected()
        );
    }

    public Page<Map<String, Object>> getMyRequests(UUID handymanId, Pageable pageable) {
        return requestRepository.findByHandymanIdOrderByCreatedAtDesc(handymanId, pageable)
                .map(this::toRequestDto);
    }

    public Map<String, Object> getTokenInfo(UUID handymanId) {
        Handyman handyman = handymanRepository.findById(handymanId)
                .orElseThrow(() -> new IllegalArgumentException("Majstor nije pronađen"));
        List<Map<String, Object>> transactions = transactionRepository
                .findByHandymanIdOrderByCreatedAtDesc(handymanId).stream()
                .map(t -> Map.<String, Object>of(
                        "id", t.getId(),
                        "amount", t.getAmount(),
                        "type", t.getType(),
                        "description", t.getDescription() != null ? t.getDescription() : "",
                        "createdAt", t.getCreatedAt()
                ))
                .toList();
        return Map.of(
                "tokenBalance", handyman.getTokenBalance(),
                "transactions", transactions
        );
    }

    @Transactional
    public void approveRequest(UUID requestId) {
        TokenPurchaseRequest request = requestRepository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Zahtev nije pronađen"));
        if (!"PENDING".equals(request.getStatus())) {
            throw new IllegalArgumentException("Zahtev je već obrađen");
        }

        Handyman handyman = request.getHandyman();
        handyman.setTokenBalance(handyman.getTokenBalance() + request.getTokenAmount());
        handymanRepository.save(handyman);

        request.setStatus("APPROVED");
        request.setProcessedAt(Instant.now());
        requestRepository.save(request);

        transactionRepository.save(TokenTransaction.builder()
                .handymanId(handyman.getId())
                .amount(request.getTokenAmount())
                .type("PURCHASED")
                .description("Odobren zahtev za tokene")
                .build());

        emailService.send(handyman.getEmail(), "Tokeni dodati",
                "Tokeni su dodati na vaš nalog. Dodato " + request.getTokenAmount() + " tokena.");
    }

    @Transactional
    public void rejectRequest(UUID requestId, String adminNote) {
        TokenPurchaseRequest request = requestRepository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Zahtev nije pronađen"));
        if (!"PENDING".equals(request.getStatus())) {
            throw new IllegalArgumentException("Zahtev je već obrađen");
        }
        request.setStatus("REJECTED");
        request.setAdminNote(adminNote);
        request.setProcessedAt(Instant.now());
        requestRepository.save(request);

        emailService.send(request.getHandyman().getEmail(), "Zahtev odbijen",
                "Vaš zahtev za tokene je odbijen. Razlog: " + (adminNote != null ? adminNote : "Nije naveden"));
    }

    @Transactional
    public void adjustTokens(UUID handymanId, int amount, String description) {
        Handyman handyman = handymanRepository.findById(handymanId)
                .orElseThrow(() -> new IllegalArgumentException("Majstor nije pronađen"));
        int newBalance = handyman.getTokenBalance() + amount;
        if (newBalance < 0) {
            throw new IllegalArgumentException("Stanje tokena ne može biti negativno");
        }
        handyman.setTokenBalance(newBalance);
        handymanRepository.save(handyman);

        transactionRepository.save(TokenTransaction.builder()
                .handymanId(handymanId)
                .amount(amount)
                .type(amount > 0 ? "PURCHASED" : "DEDUCTED")
                .description(description != null ? description : "Ručna korekcija admina")
                .build());
    }

    private Map<String, Object> toRequestDto(TokenPurchaseRequest r) {
        return Map.of(
                "id", r.getId(),
                "packageId", r.getTokenPackage() != null ? r.getTokenPackage().getId() : 0,
                "tokenAmount", r.getTokenAmount(),
                "amountExpected", r.getAmountExpected(),
                "paymentReference", r.getPaymentReference() != null ? r.getPaymentReference() : "",
                "status", r.getStatus(),
                "adminNote", r.getAdminNote() != null ? r.getAdminNote() : "",
                "createdAt", r.getCreatedAt()
        );
    }
}
