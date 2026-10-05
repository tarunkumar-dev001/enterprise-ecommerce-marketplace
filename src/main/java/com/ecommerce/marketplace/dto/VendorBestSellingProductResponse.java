package com.ecommerce.marketplace.dto;

import java.math.BigDecimal;

public record VendorBestSellingProductResponse(
        Long productId,
        String productName,
        Long unitsSold,
        BigDecimal salesRevenue) {
}