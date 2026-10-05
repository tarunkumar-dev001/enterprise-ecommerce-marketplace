package com.ecommerce.marketplace.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record CheckoutItemRequest(
        @NotNull @Positive Long productId,
        @NotNull @Positive Integer quantity) {
}
