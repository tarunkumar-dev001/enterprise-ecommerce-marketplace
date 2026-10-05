package com.ecommerce.marketplace.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record ProductRequest(
        @NotBlank(message = "Product name is required") String name,
        @NotBlank(message = "Product description is required") String description,
        @NotNull(message = "Product price is required")
        @DecimalMin(value = "0.01", message = "Product price must be greater than zero")
        BigDecimal price,
        @NotNull(message = "Stock is required")
        @Min(value = 0, message = "Stock must be zero or greater") Integer stock,
        @NotBlank(message = "Category is required") String category,
        @jakarta.validation.constraints.Size(max = 2048, message = "Image URL must be 2048 characters or fewer") String imageUrl) {
}

