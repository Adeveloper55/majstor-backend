package com.majstornaklik.dto;

import com.majstornaklik.entity.ContactMessage;

import java.time.Instant;
import java.util.UUID;

public record ContactMessageDto(
        UUID id,
        String fullName,
        String email,
        String phone,
        String message,
        boolean isContractor,
        String status,
        Instant createdAt
) {
    public static ContactMessageDto from(ContactMessage m) {
        return new ContactMessageDto(
                m.getId(),
                m.getFullName(),
                m.getEmail(),
                m.getPhone(),
                m.getMessage(),
                Boolean.TRUE.equals(m.getIsContractor()),
                m.getStatus(),
                m.getCreatedAt()
        );
    }
}
