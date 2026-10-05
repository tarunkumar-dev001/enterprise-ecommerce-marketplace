package com.ecommerce.marketplace.event;

import java.math.BigDecimal;

public record OrderPlacedEvent(String customerEmail, Long orderId, BigDecimal totalAmount) {
}