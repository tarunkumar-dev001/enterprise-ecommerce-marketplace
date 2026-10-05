package com.ecommerce.marketplace.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public record AdminDashboardResponse(long totalUsers, long totalVendors, long totalCustomers, long totalProducts,
        long totalOrders, BigDecimal totalRevenue, BigDecimal pendingPayouts, BigDecimal platformCommission,
        Map<String, Long> orderStatuses, List<OrderResponse> recentOrders, List<AdminProductResponse> topProducts,
        List<AdminVendorResponse> topVendors, List<ProductReviewResponse> recentReviews) { }
