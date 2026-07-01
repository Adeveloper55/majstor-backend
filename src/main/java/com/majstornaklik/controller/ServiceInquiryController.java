package com.majstornaklik.controller;

import com.majstornaklik.dto.ServiceInquiryDto;
import com.majstornaklik.dto.ServiceInquiryRequest;
import com.majstornaklik.service.ServiceInquiryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/inquiries")
@RequiredArgsConstructor
public class ServiceInquiryController {

    private final ServiceInquiryService serviceInquiryService;

    @PostMapping
    public ServiceInquiryDto submit(@Valid @RequestBody ServiceInquiryRequest req) {
        return serviceInquiryService.submit(req);
    }
}
