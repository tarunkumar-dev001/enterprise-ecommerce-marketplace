package com.ecommerce.marketplace.dto;

import com.ecommerce.marketplace.entity.Product;
import com.ecommerce.marketplace.entity.ProductModerationStatus;

import java.math.BigDecimal;

public record ProductResponse(
        Long id,
        String name,
        String description,
        BigDecimal price,
        Integer stock,
        String category,
        String imageUrl,
        Long vendorId,
        String vendorUsername,
        ProductModerationStatus moderationStatus,
        String moderationReason) {

    public static ProductResponse from(Product product) {
        return new ProductResponse(
                product.getId(),
                product.getName(),
                product.getDescription(),
                product.getPrice(),
                product.getStock(),
                product.getCategory(),
                product.getImageUrl(),
                product.getVendor().getId(),
                product.getVendor().getUsername(),
                product.getModerationStatus(),
                product.getModerationReason());
    }
}


