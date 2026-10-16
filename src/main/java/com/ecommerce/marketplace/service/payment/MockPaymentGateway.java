package com.ecommerce.marketplace.service.payment;

import com.ecommerce.marketplace.entity.PaymentStatus;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;

@Component
public class MockPaymentGateway implements PaymentGateway {

    @Override
    public PaymentResult charge(Long orderId, BigDecimal amount, boolean simulateFailure) {
        if (orderId == null || orderId < 1) {
            throw new IllegalArgumentException("A valid order ID is required for payment");
        }
        if (amount == null || amount.signum() <= 0) {
            throw new IllegalArgumentException("Payment amount must be greater than zero");
        }

        PaymentStatus status = simulateFailure ? PaymentStatus.FAILED : PaymentStatus.PAID;
        String outcome = simulateFailure ? "failed" : "paid";
        return new PaymentResult(status, "portfolio_" + outcome + "_" + orderId + "_" + UUID.randomUUID());
    }
}
