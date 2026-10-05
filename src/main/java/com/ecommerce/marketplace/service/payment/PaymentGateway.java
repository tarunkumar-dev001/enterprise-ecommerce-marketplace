package com.ecommerce.marketplace.service.payment;

import java.math.BigDecimal;

public interface PaymentGateway {

    PaymentResult charge(Long orderId, BigDecimal amount, boolean simulateFailure);
}