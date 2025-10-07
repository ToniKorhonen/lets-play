package com.Zone01.lets_play.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;

public class ProductDtos {
    public record CreateProductRequest(
            @NotBlank String name,
            String description,
            @PositiveOrZero double price
    ) {}
    public record UpdateProductRequest(
            @NotBlank String name,
            String description,
            @PositiveOrZero double price
    ) {}
    public record ProductResponse(
            String id,
            String name,
            String description,
            double price,
            String userId,
            String ownerName
    ) {}
}
