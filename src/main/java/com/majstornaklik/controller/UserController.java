package com.majstornaklik.controller;

import com.majstornaklik.dto.DtoMapper;
import com.majstornaklik.entity.User;
import com.majstornaklik.repository.UserRepository;
import com.majstornaklik.security.SecurityUtils;
import com.majstornaklik.service.PhoneUniquenessService;
import com.majstornaklik.util.PhoneUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserRepository userRepository;
    private final SecurityUtils securityUtils;
    private final PhoneUniquenessService phoneUniquenessService;

    @GetMapping("/me")
    public DtoMapper.UserDto getMe() {
        securityUtils.requireRole("ROLE_CLIENT");
        return DtoMapper.toUserDto(getCurrentUser());
    }

    @PutMapping("/me")
    public DtoMapper.UserDto updateMe(@RequestBody Map<String, Object> body) {
        securityUtils.requireRole("ROLE_CLIENT");
        User user = getCurrentUser();
        updateFields(user, body);
        return DtoMapper.toUserDto(userRepository.save(user));
    }

    @GetMapping("/{id}")
    public DtoMapper.UserDto getPublic(@PathVariable UUID id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Korisnik nije pronađen"));
        return DtoMapper.toUserDto(user);
    }

    @DeleteMapping("/me")
    public void deactivate() {
        securityUtils.requireRole("ROLE_CLIENT");
        User user = getCurrentUser();
        user.setIsActive(false);
        userRepository.save(user);
    }

    private User getCurrentUser() {
        return userRepository.findById(securityUtils.getCurrentUserId())
                .orElseThrow(() -> new IllegalArgumentException("Korisnik nije pronađen"));
    }

    private void updateFields(User user, Map<String, Object> body) {
        if (body.containsKey("fullName")) user.setFullName((String) body.get("fullName"));
        if (body.containsKey("phone")) {
            String phoneNormalized = PhoneUtils.normalizeOptional((String) body.get("phone"));
            phoneUniquenessService.assertPhoneAvailable(phoneNormalized, user.getId(), null, null);
            user.setPhone(phoneNormalized);
            user.setPhoneNormalized(phoneNormalized);
        }
        if (body.containsKey("city")) user.setCity((String) body.get("city"));
        if (body.containsKey("address")) user.setAddress((String) body.get("address"));
        if (body.containsKey("profileImageUrl")) user.setProfileImageUrl((String) body.get("profileImageUrl"));
        if (body.containsKey("latitude")) user.setLatitude(toDouble(body.get("latitude")));
        if (body.containsKey("longitude")) user.setLongitude(toDouble(body.get("longitude")));
    }

    private Double toDouble(Object value) {
        if (value == null) return null;
        if (value instanceof Number n) return n.doubleValue();
        return Double.parseDouble(value.toString());
    }
}
