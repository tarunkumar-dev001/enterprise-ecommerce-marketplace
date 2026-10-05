package com.ecommerce.marketplace.event;

import com.ecommerce.marketplace.entity.OrderStatus;

public record OrderStatusUpdatedEvent(String customerEmail, Long orderId, OrderStatus status) {
}