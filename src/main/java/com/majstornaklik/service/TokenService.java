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
import com.majstornaklik.util.IpsQrCodeGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashMap;
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

    @Value("${app.company.payment-purpose:Uplata za tokene}")
    private String companyPaymentPurpose;

    @Value("${app.company.payment-reference:365000001}")
    private String companyPaymentReference;

    @Value("${app.company.payment-model:97}")
    private String paymentModel;

    @Value("${app.frontend.url:http://localhost:3000}")
    private String frontendUrl;

    public Page<TokenPackage> listPackages(Pageable pageable) {
        return packageRepository.findByIsActiveTrue(pageable);
    }

    public Map<String, String> getBankDetails() {
        Map<String, String> details = new HashMap<>();
        details.put("companyName", companyName);
        details.put("companyPib", companyPib != null ? companyPib : "");
        details.put("bankName", bankName);
        details.put("bankAccount", bankAccount);
        details.put("paymentPurpose", companyPaymentPurpose);
        details.put("paymentReference", companyPaymentReference);
        details.put("paymentReferenceDisplay", IpsQrCodeGenerator.formatPozivNaBrojDisplay(paymentModel, companyPaymentReference));
        details.put("paymentInstructions", paymentInstructions);
        return details;
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

        String paymentReference = dto.paymentReference();
        if (paymentReference == null || paymentReference.isBlank()) {
            paymentReference = handyman.getEmail();
        }
        String pozivDisplay = IpsQrCodeGenerator.formatPozivNaBrojDisplay(paymentModel, companyPaymentReference);

        TokenPurchaseRequest request = TokenPurchaseRequest.builder()
                .handyman(handyman)
                .tokenPackage(pkg)
                .tokenAmount(pkg.getTokenAmount())
                .amountExpected(pkg.getPriceEur())
                .paymentReference(paymentReference)
                .status("PENDING")
                .build();
        requestRepository.save(request);

        emailService.sendSafely(
                handyman.getEmail(),
                "Zahtev za tokene primljen",
                """
                Poštovani %s,

                Primili smo vaš zahtev za %d tokena (paket: %s, iznos: %s RSD).

                Admin će uskoro poslati predračun sa IPS QR kodom i podacima za uplatu na ovaj email.
                Nakon uplate, admin potvrđuje uplatu i tokeni se dodaju na vaš nalog.

                Primalac: %s
                Svrha uplate: %s
                Poziv na broj: %s
                """.formatted(
                        handyman.getFullName(),
                        pkg.getTokenAmount(),
                        pkg.getName(),
                        pkg.getPriceEur(),
                        companyName,
                        companyPaymentPurpose,
                        pozivDisplay
                )
        );

        emailService.sendToAdmin(
                "Novi zahtev za tokene",
                """
                Majstor/izvođač: %s (%s)
                Paket: %d tokena
                Iznos: %s RSD
                Svrha uplate: %s
                Poziv na broj: %s
                ID zahteva: %s

                Otvori admin panel i klikni „Pošalji predračun“:
                %s/admin/token-requests
                """.formatted(
                        handyman.getFullName(),
                        handyman.getEmail(),
                        pkg.getTokenAmount(),
                        pkg.getPriceEur(),
                        companyPaymentPurpose,
                        pozivDisplay,
                        request.getId(),
                        frontendUrl
                )
        );

        return Map.of(
                "id", request.getId(),
                "status", request.getStatus(),
                "tokenAmount", request.getTokenAmount(),
                "amountExpected", request.getAmountExpected()
        );
    }

    @Transactional
    public void sendPredracun(UUID requestId) {
        TokenPurchaseRequest request = requestRepository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Zahtev nije pronađen"));
        if (!"PENDING".equals(request.getStatus())) {
            throw new IllegalArgumentException("Predračun se može poslati samo za zahteve na čekanju");
        }

        Handyman handyman = request.getHandyman();
        String paymentPurpose = companyPaymentPurpose;
        String pozivNaBroj = companyPaymentReference;
        String pozivDisplay = IpsQrCodeGenerator.formatPozivNaBrojDisplay(paymentModel, pozivNaBroj);

        String qrPayload = IpsQrCodeGenerator.buildPaymentPayload(
                bankAccount,
                companyName,
                request.getAmountExpected(),
                paymentPurpose,
                paymentModel,
                pozivNaBroj
        );

        byte[] qrPng;
        try {
            qrPng = IpsQrCodeGenerator.generatePng(qrPayload, 280);
        } catch (Exception e) {
            throw new IllegalStateException("Greška pri generisanju IPS QR koda: " + e.getMessage(), e);
        }

        String pibLine = companyPib != null && !companyPib.isBlank()
                ? "<tr><td style=\"padding:6px 0;color:#64748b;\">PIB</td><td style=\"padding:6px 0;font-weight:600;\">" + escapeHtml(companyPib) + "</td></tr>"
                : "";

        String html = """
                <div style="font-family:Arial,sans-serif;max-width:560px;color:#0f172a;">
                  <h2 style="margin:0 0 12px;">Predračun za dopunu tokena</h2>
                  <p style="margin:0 0 16px;color:#475569;">Poštovani %s,</p>
                  <p style="margin:0 0 16px;color:#475569;">U prilogu su podaci za uplatu paketa <strong>%d tokena</strong>.</p>
                  <table style="width:100%%;border-collapse:collapse;margin-bottom:20px;">
                    <tr><td style="padding:6px 0;color:#64748b;">Primalac</td><td style="padding:6px 0;font-weight:600;">%s</td></tr>
                    %s
                    <tr><td style="padding:6px 0;color:#64748b;">Račun</td><td style="padding:6px 0;font-weight:600;">%s</td></tr>
                    <tr><td style="padding:6px 0;color:#64748b;">Iznos</td><td style="padding:6px 0;font-weight:600;">%s RSD</td></tr>
                    <tr><td style="padding:6px 0;color:#64748b;">Svrha uplate</td><td style="padding:6px 0;font-weight:600;">%s</td></tr>
                    <tr><td style="padding:6px 0;color:#64748b;">Poziv na broj</td><td style="padding:6px 0;font-weight:600;">%s</td></tr>
                  </table>
                  <p style="margin:0 0 12px;color:#475569;">Skenirajte IPS QR kod u m-banking aplikaciji:</p>
                  <p style="margin:0 0 20px;"><img src="cid:ipsQr" alt="IPS QR kod" width="220" height="220" style="border:1px solid #e2e8f0;border-radius:8px;" /></p>
                  <p style="margin:0 0 8px;color:#475569;font-size:14px;">%s</p>
                  <p style="margin:16px 0 0;color:#64748b;font-size:13px;">Nakon uplate, admin će proveriti uplatu i dodati tokene na vaš nalog.</p>
                </div>
                """.formatted(
                escapeHtml(handyman.getFullName()),
                request.getTokenAmount(),
                escapeHtml(companyName),
                pibLine,
                escapeHtml(formatAccountDisplay(bankAccount)),
                request.getAmountExpected(),
                escapeHtml(paymentPurpose),
                escapeHtml(pozivDisplay),
                escapeHtml(paymentInstructions != null ? paymentInstructions : "")
        );

        emailService.sendHtmlWithInlineImageSafely(
                handyman.getEmail(),
                "Predračun — dopuna tokena",
                html,
                "ipsQr",
                qrPng,
                "image/png"
        );

        request.setPredracunSentAt(Instant.now());
        requestRepository.save(request);
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

        emailService.sendSafely(handyman.getEmail(), "Tokeni dodati",
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

        emailService.sendSafely(request.getHandyman().getEmail(), "Zahtev odbijen",
                "Vaš zahtev za tokene je odbijen. Razlog: " + (adminNote != null ? adminNote : "Nije naveden"));
    }

    @Transactional
    public void adjustTokens(UUID handymanId, int amount, String description) {
        Handyman handyman = handymanRepository.findById(handymanId)
                .orElseThrow(() -> new IllegalArgumentException("Majstor nije pronađen"));
        int currentBalance = handyman.getTokenBalance() != null ? handyman.getTokenBalance() : 0;
        if (amount < 0 && currentBalance == 0) {
            throw new IllegalArgumentException("Stanje tokena je nula.");
        }
        int newBalance = currentBalance + amount;
        if (newBalance < 0) {
            throw new IllegalArgumentException("Nedovoljno tokena na nalogu.");
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
        Map<String, Object> dto = new HashMap<>();
        dto.put("id", r.getId());
        dto.put("packageId", r.getTokenPackage() != null ? r.getTokenPackage().getId() : 0);
        dto.put("tokenAmount", r.getTokenAmount());
        dto.put("amountExpected", r.getAmountExpected());
        dto.put("paymentReference", r.getPaymentReference() != null ? r.getPaymentReference() : "");
        dto.put("status", r.getStatus());
        dto.put("adminNote", r.getAdminNote() != null ? r.getAdminNote() : "");
        dto.put("createdAt", r.getCreatedAt());
        dto.put("predracunSentAt", r.getPredracunSentAt());
        return dto;
    }

    private static String formatAccountDisplay(String account) {
        String digits = account.replaceAll("\\D", "");
        if (digits.length() != 18) {
            return account;
        }
        return digits.substring(0, 3) + "-"
                + digits.substring(3, 16) + "-"
                + digits.substring(16);
    }

    private static String escapeHtml(String value) {
        if (value == null) {
            return "";
        }
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }
}
