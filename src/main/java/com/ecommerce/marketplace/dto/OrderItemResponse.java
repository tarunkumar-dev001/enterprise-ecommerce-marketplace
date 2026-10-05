package com.ecommerce.marketplace.dto;

import com.ecommerce.marketplace.entity.OrderItem;

import java.math.BigDecimal;

public record OrderItemResponse(Long id, Long productId, String productName, String imageUrl, Integer quantity, BigDecimal unitPrice, BigDecimal subtotal) {
    public static OrderItemResponse from(OrderItem item) {
        BigDecimal subtotal = item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
        return new OrderItemResponse(
                item.getId(),
                item.getProduct().getId(),
                item.getProduct().getName(),
                item.getProduct().getImageUrl(),
                item.getQuantity(),
                item.getUnitPrice(),
                subtotal);
    }
}
