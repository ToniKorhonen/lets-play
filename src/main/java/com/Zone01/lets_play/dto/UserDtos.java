package com.Zone01.lets_play.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class UserDtos {
    public record CreateUserRequest(
            @NotBlank String name,
            @NotBlank @Email String email,
            @NotBlank @Size(min = 8) String password,
            String role
    ) {}

    public record UpdateUserRequest(
            @NotBlank String name,
            String role // optionnel, contrôlé côté service (seulement ADMIN)
    ) {}

    public record UserResponse(
            String id,
            String name,
            String email,
            String role
    ) {}
}
