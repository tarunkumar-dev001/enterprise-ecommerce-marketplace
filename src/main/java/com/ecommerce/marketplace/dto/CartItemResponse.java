package com.ecommerce.marketplace.dto;

import com.ecommerce.marketplace.entity.CartItem;

import java.math.BigDecimal;

public record CartItemResponse(
        Long id,
        Long productId,
        String productName,
        String imageUrl,
        Long vendorId,
        String vendorUsername,
        Integer quantity,
        BigDecimal unitPrice,
        BigDecimal subtotal) {

    public static CartItemResponse from(CartItem item) {
        BigDecimal subtotal = item.getProduct().getPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
        return new CartItemResponse(
                item.getId(),
                item.getProduct().getId(),
                item.getProduct().getName(),
                item.getProduct().getImageUrl(),
                item.getProduct().getVendor().getId(),
                item.getProduct().getVendor().getUsername(),
                item.getQuantity(),
                item.getProduct().getPrice(),
                subtotal);
    }
}
