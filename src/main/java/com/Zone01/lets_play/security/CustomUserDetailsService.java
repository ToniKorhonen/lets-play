package com.Zone01.lets_play.security;

import com.Zone01.lets_play.Mongo_repisitory.UserRepository;
import com.Zone01.lets_play.models.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.List;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private static final Logger log = LoggerFactory.getLogger(CustomUserDetailsService.class);
    private final UserRepository users;

    public CustomUserDetailsService(UserRepository users) {
        this.users = users;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        log.debug("[LOGIN] Loading user by email='{}'", email);
        User u = users.findByEmail(email);
        if (u == null) {
            log.warn("[LOGIN] User not found for email='{}'", email);
            throw new UsernameNotFoundException("User not found");
        }
        // Role is stored without ROLE_ prefix (e.g., "USER", "ADMIN")
        // Spring Security expects ROLE_ prefix in authorities
        String role = (u.getRole() == null || u.getRole().isBlank()) ? "USER" : u.getRole().toUpperCase();
        // Remove ROLE_ prefix if already present, then add it
        role = role.replace("ROLE_", "");
        List<GrantedAuthority> auth = List.of(new SimpleGrantedAuthority("ROLE_" + role));
        log.debug("[LOGIN] User loaded: id={}, email={}, role={}", u.getId(), u.getEmail(), role);
        return org.springframework.security.core.userdetails.User
                .withUsername(u.getEmail())
                .password(u.getPassword())
                .authorities(auth)
                .build();
    }
}
