package com.ecommerce.marketplace.service.payment;

import com.ecommerce.marketplace.entity.PaymentStatus;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;

@Component
public class MockPaymentGateway implements PaymentGateway {

    @Override
    public PaymentResult charge(Long orderId, BigDecimal amount, boolean simulateFailure) {
        PaymentStatus status = simulateFailure ? PaymentStatus.FAILED : PaymentStatus.PAID;
        return new PaymentResult(status, "mock_txn_" + UUID.randomUUID());
    }
}