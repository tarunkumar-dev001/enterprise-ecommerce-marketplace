package com.ecommerce.marketplace.dto;

import com.ecommerce.marketplace.entity.Product;

import java.math.BigDecimal;

public record ProductDetailsResponse(
        Long id,
        String name,
        String description,
        BigDecimal price,
        Integer stock,
        String category,
        String imageUrl,
        Long vendorId,
        String vendorUsername,
        double averageRating,
        long reviewCount) {

    public static ProductDetailsResponse from(Product product) {
        ProductResponse productResponse = ProductResponse.from(product);
        return new ProductDetailsResponse(
                productResponse.id(),
                productResponse.name(),
                productResponse.description(),
                productResponse.price(),
                productResponse.stock(),
                productResponse.category(),
                productResponse.imageUrl(),
                productResponse.vendorId(),
                productResponse.vendorUsername(),
                product.getAverageRating().doubleValue(),
                product.getReviewCount());
    }
}

