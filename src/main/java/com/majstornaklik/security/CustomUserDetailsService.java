package com.majstornaklik.security;

import com.majstornaklik.repository.AdminRepository;
import com.majstornaklik.repository.HandymanRepository;
import com.majstornaklik.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;
    private final HandymanRepository handymanRepository;
    private final AdminRepository adminRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        return userRepository.findByEmail(email)
                .map(u -> new UserPrincipal(u.getId(), u.getEmail(), u.getPasswordHash(), "ROLE_CLIENT", u.getIsActive()))
                .or(() -> handymanRepository.findByEmail(email)
                        .map(h -> new UserPrincipal(h.getId(), h.getEmail(), h.getPasswordHash(), "ROLE_HANDYMAN", h.getIsActive())))
                .or(() -> adminRepository.findByEmail(email)
                        .map(a -> new UserPrincipal(a.getId(), a.getEmail(), a.getPasswordHash(), "ROLE_ADMIN", a.getIsActive())))
                .orElseThrow(() -> new UsernameNotFoundException("Korisnik nije pronađen"));
    }
}
