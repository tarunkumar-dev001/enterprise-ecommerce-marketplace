package com.ecommerce.marketplace.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

public record CheckoutRequest(
        @NotEmpty @Size(max = 100) List<@Valid CheckoutItemRequest> items,
        @Size(max = 32) String couponCode,
        @Valid ShippingAddress shippingAddress) {

    public CheckoutRequest(List<CheckoutItemRequest> items, String couponCode) {
        this(items, couponCode, null);
    }
}
