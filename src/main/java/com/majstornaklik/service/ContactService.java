package com.majstornaklik.service;

import com.majstornaklik.dto.ContactMessageDto;
import com.majstornaklik.dto.ContactMessageRequest;
import com.majstornaklik.entity.ContactMessage;
import com.majstornaklik.repository.ContactMessageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ContactService {

    private final ContactMessageRepository repository;
    private final EmailService emailService;

    @Transactional
    public ContactMessageDto submit(ContactMessageRequest req) {
        ContactMessage message = ContactMessage.builder()
                .fullName(req.fullName())
                .email(req.email())
                .phone(req.phone())
                .message(req.message())
                .isContractor(Boolean.TRUE.equals(req.isContractor()))
                .status("NEW")
                .build();
        repository.save(message);

        String type = Boolean.TRUE.equals(req.isContractor()) ? "majstor/izvođač" : "korisnik";
        emailService.sendToAdmin("Nova kontakt poruka",
                "Od: " + req.fullName() + " (" + type + ")\nEmail: " + req.email() + "\n\n" + req.message());

        return ContactMessageDto.from(message);
    }

    public Page<ContactMessageDto> listAll(Pageable pageable) {
        return repository.findAllByOrderByCreatedAtDesc(pageable).map(ContactMessageDto::from);
    }

    public ContactMessageDto get(UUID id) {
        ContactMessage m = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Poruka nije pronađena"));
        return ContactMessageDto.from(m);
    }

    @Transactional
    public ContactMessageDto markRead(UUID id) {
        ContactMessage m = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Poruka nije pronađena"));
        m.setStatus("READ");
        repository.save(m);
        return ContactMessageDto.from(m);
    }

    @Transactional
    public void delete(UUID id) {
        repository.deleteById(id);
    }
}
