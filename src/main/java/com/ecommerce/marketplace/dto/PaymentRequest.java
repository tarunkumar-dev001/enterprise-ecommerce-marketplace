package com.ecommerce.marketplace.dto;

import jakarta.validation.constraints.NotNull;

public record PaymentRequest(
        @NotNull(message = "simulateFailure is required")
        Boolean simulateFailure) {
}