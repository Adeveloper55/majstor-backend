package com.majstornaklik.controller;

import com.majstornaklik.dto.ContactMessageDto;
import com.majstornaklik.dto.ContactMessageRequest;
import com.majstornaklik.service.ContactService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/contact")
@RequiredArgsConstructor
public class ContactController {

    private final ContactService contactService;

    @PostMapping
    public ContactMessageDto submit(@Valid @RequestBody ContactMessageRequest req) {
        return contactService.submit(req);
    }
}
