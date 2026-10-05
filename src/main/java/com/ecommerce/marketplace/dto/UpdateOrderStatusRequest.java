package com.ecommerce.marketplace.dto;

import com.ecommerce.marketplace.entity.OrderStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateOrderStatusRequest(@NotNull(message = "Order status is required") OrderStatus status) {
}
