package com.ecommerce.marketplace.dto;

import jakarta.validation.constraints.NotNull;

public record WishlistItemRequest(@NotNull(message = "Product ID is required") Long productId) {
}