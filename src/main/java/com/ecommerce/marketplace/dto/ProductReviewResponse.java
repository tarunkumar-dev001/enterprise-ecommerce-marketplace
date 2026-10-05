package com.ecommerce.marketplace.dto;

import com.ecommerce.marketplace.entity.ProductReview;

import java.time.LocalDateTime;

public record ProductReviewResponse(
        Long id,
        Long productId,
        Long customerId,
        String customerUsername,
        Integer rating,
        String text,
        LocalDateTime createdAt,
        String productName,
        String vendorUsername) {

    public static ProductReviewResponse from(ProductReview review) {
        return new ProductReviewResponse(
                review.getId(),
                review.getProduct().getId(),
                review.getCustomer().getId(),
                review.getCustomer().getUsername(),
                review.getRating(),
                review.getText(),
                review.getCreatedAt(),
                review.getProduct().getName(),
                review.getProduct().getVendor().getUsername());
    }
}
