package com.Zone01.lets_play.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Pattern;

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

    // New DTO to update the user's own profile
    public record UpdateProfileRequest(
            String name,
            @Size(min = 8, message = "Password must be at least 8 characters")
            @Pattern(regexp = "^(?=.*[0-9])(?=.*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/?]).*$",
                    message = "Password must contain at least one number and one special character")
            String password
    ) {}

    public record UserResponse(
            String id,
            String name,
            String email,
            String role
    ) {}
}
