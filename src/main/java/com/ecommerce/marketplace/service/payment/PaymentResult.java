package com.ecommerce.marketplace.service.payment;

import com.ecommerce.marketplace.entity.PaymentStatus;

public record PaymentResult(PaymentStatus status, String transactionReference) {
}