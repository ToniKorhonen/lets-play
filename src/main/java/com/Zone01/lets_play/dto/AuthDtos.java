package com.Zone01.lets_play.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public class AuthDtos {
    public record LoginRequest(
        @NotBlank @Email String email,
        @NotBlank String password
    ) {}

    public record LoginResponse(
        String token
    ) {}
}

