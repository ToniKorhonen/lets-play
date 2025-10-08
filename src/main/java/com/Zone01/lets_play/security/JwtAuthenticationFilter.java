package com.Zone01.lets_play.security;

import com.Zone01.lets_play.Mongo_repisitory.UserRepository;
import io.jsonwebtoken.Claims;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);
    private final JwtService jwtService;

    public JwtAuthenticationFilter(JwtService jwtService, UserRepository userRepository) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7);
            try {
                Claims claims = jwtService.parseToken(token);
                String email = claims.getSubject();
                String role = claims.get("role", String.class);

                if (email != null && role != null) {
                    // Role in JWT is stored without ROLE_ prefix (e.g., "USER", "ADMIN")
                    // Spring Security needs ROLE_ prefix
                    String normalizedRole = role.replace("ROLE_", "").toUpperCase();
                    UserDetails userDetails = org.springframework.security.core.userdetails.User
                            .withUsername(email)
                            .password("")
                            .authorities(new SimpleGrantedAuthority("ROLE_" + normalizedRole))
                            .build();
                    UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                            userDetails, null, userDetails.getAuthorities());
                    auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(auth);
                }
            } catch (Exception e) {
                log.error("JWT Authentication failed: {}", e.getMessage());
                SecurityContextHolder.clearContext();
            }
        }
        chain.doFilter(request, response);
    }
}
