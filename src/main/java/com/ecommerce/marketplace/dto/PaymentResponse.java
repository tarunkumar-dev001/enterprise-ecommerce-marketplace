package com.ecommerce.marketplace.dto;

import com.ecommerce.marketplace.entity.PaymentStatus;

import java.math.BigDecimal;

public record PaymentResponse(
        Long orderId,
        BigDecimal amount,
        PaymentStatus paymentStatus,
        String transactionReference,
        String message) {
}