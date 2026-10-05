package com.ecommerce.marketplace.dto;

import java.math.BigDecimal;
import java.util.List;

public record VendorAnalyticsResponse(
        BigDecimal totalSalesRevenue,
        long orderCount,
        long unitsSold,
        List<VendorBestSellingProductResponse> bestSellingProducts) {
}