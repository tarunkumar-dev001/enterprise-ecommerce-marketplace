package com.ecommerce.marketplace.dto;

import com.ecommerce.marketplace.entity.Product;
import com.ecommerce.marketplace.entity.ProductModerationStatus;
import java.math.BigDecimal;

public record AdminProductResponse(Long id, String name, String category, String imageUrl, Long vendorId,
        String vendorUsername, BigDecimal price, Integer stock, BigDecimal averageRating, long reviewCount, long unitsSold,
        String status, ProductModerationStatus moderationStatus, String moderationReason, String stockStatus) {
    public static AdminProductResponse from(Product product) {
        String stockStatus = product.getStock() > 0 ? "ACTIVE" : "OUT_OF_STOCK";
        return new AdminProductResponse(product.getId(), product.getName(), product.getCategory(), product.getImageUrl(),
                product.getVendor().getId(), product.getVendor().getUsername(), product.getPrice(), product.getStock(),
                product.getAverageRating(), product.getReviewCount(), 0L, stockStatus,
                product.getModerationStatus(), product.getModerationReason(), stockStatus);
    }
}
