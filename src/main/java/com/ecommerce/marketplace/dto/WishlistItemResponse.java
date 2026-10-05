package com.ecommerce.marketplace.dto;

import com.ecommerce.marketplace.entity.WishlistItem;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record WishlistItemResponse(
        Long productId,
        String productName,
        String imageUrl,
        BigDecimal price,
        String category,
        Integer stock,
        String vendorUsername,
        LocalDateTime addedAt) {

    public static WishlistItemResponse from(WishlistItem item) {
        return new WishlistItemResponse(
                item.getProduct().getId(),
                item.getProduct().getName(),
                item.getProduct().getImageUrl(),
                item.getProduct().getPrice(),
                item.getProduct().getCategory(),
                item.getProduct().getStock(),
                item.getProduct().getVendor().getUsername(),
                item.getAddedAt());
    }
}