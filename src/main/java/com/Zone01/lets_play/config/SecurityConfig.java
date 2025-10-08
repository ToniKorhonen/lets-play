package com.Zone01.lets_play.config;

import com.Zone01.lets_play.security.JwtAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.config.http.SessionCreationPolicy;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.Map;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfigurationSource;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final CorsConfigurationSource corsConfigurationSource;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter,
                          CorsConfigurationSource corsConfigurationSource) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.corsConfigurationSource = corsConfigurationSource;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public AuthenticationEntryPoint restAuthenticationEntryPoint() {
        return (request, response, authException) -> {
            response.setContentType("application/json");
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            Map<String, Object> body = new HashMap<>();
            body.put("status", 401);
            body.put("error", "Unauthorized");
            body.put("message", authException.getMessage());
            body.put("path", request.getRequestURI());
            new ObjectMapper().writeValue(response.getOutputStream(), body);
        };
    }

    @Bean
    public AccessDeniedHandler restAccessDeniedHandler() {
        return (request, response, accessDeniedException) -> {
            response.setContentType("application/json");
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            Map<String, Object> body = new HashMap<>();
            body.put("status", 403);
            body.put("error", "Forbidden");
            body.put("message", accessDeniedException.getMessage());
            body.put("path", request.getRequestURI());
            new ObjectMapper().writeValue(response.getOutputStream(), body);
        };
    }

    @Bean
    @Order(1)
    public SecurityFilterChain apiFilterChain(HttpSecurity http) throws Exception {
        http
            .securityMatcher("/api/**")
            .cors(cors -> cors.configurationSource(corsConfigurationSource))
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .csrf(AbstractHttpConfigurer::disable)
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
            .authorizeHttpRequests(auth -> auth
                // Public endpoints
                .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/products/**").permitAll()
                .requestMatchers(org.springframework.http.HttpMethod.POST, "/api/auth/login").permitAll()
                .requestMatchers(org.springframework.http.HttpMethod.POST, "/api/users").permitAll()

                // Admin-only endpoints
                .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/users").hasRole("ADMIN")
                .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/users/*").hasRole("ADMIN")
                .requestMatchers(org.springframework.http.HttpMethod.PUT, "/api/users/*").hasRole("ADMIN")
                // DELETE users - permettre aux utilisateurs authentifiés (le contrôleur vérifie si c'est le bon utilisateur)
                .requestMatchers(org.springframework.http.HttpMethod.DELETE, "/api/users/*").authenticated()

                // Authenticated user endpoints
                .requestMatchers("/api/products/**").authenticated()
                .anyRequest().authenticated()
            )
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint(restAuthenticationEntryPoint())
                .accessDeniedHandler(restAccessDeniedHandler())
            );
        return http.build();
    }

    @Bean
    @Order(2)
    public SecurityFilterChain webFilterChain(HttpSecurity http) throws Exception {
        http
            .securityMatcher("/**")
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/", "/login", "/register", "/products", "/auth/register", "/css/**", "/js/**", "/static/**").permitAll()
                .requestMatchers("/web/admin/**").hasRole("ADMIN")
                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginPage("/login")
                .loginProcessingUrl("/login")
                .usernameParameter("email")
                .passwordParameter("password")
                .defaultSuccessUrl("/", true)
                .failureUrl("/login?error=true")
                .permitAll()
            )
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/login?logout=true")
                .permitAll()
            );
        return http.build();
    }
}