package com.majstornaklik.controller;

import com.majstornaklik.dto.RejectTokenRequest;
import com.majstornaklik.dto.TokenRequestDto;
import com.majstornaklik.entity.TokenPackage;
import com.majstornaklik.security.SecurityUtils;
import com.majstornaklik.service.TokenService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/tokens")
@RequiredArgsConstructor
public class TokenController {

    private final TokenService tokenService;
    private final SecurityUtils securityUtils;

    @GetMapping("/packages")
    public Page<TokenPackage> listPackages(Pageable pageable) {
        return tokenService.listPackages(pageable);
    }

    @GetMapping("/bank-details")
    public Map<String, String> bankDetails() {
        securityUtils.requireRole("ROLE_HANDYMAN");
        return tokenService.getBankDetails();
    }

    @PostMapping("/request")
    public Map<String, Object> submitRequest(@Valid @RequestBody TokenRequestDto dto) {
        securityUtils.requireRole("ROLE_HANDYMAN");
        return tokenService.submitRequest(securityUtils.getCurrentUserId(), dto);
    }

    @GetMapping("/requests")
    public Page<Map<String, Object>> myRequests(Pageable pageable) {
        securityUtils.requireRole("ROLE_HANDYMAN");
        return tokenService.getMyRequests(securityUtils.getCurrentUserId(), pageable);
    }
}
