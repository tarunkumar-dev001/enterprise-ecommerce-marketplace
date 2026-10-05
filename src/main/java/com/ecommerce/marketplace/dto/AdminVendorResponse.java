package com.ecommerce.marketplace.dto;

import java.math.BigDecimal;

public record AdminVendorResponse(Long id, String username, String email, long productCount, long orderCount,
        BigDecimal grossRevenue, BigDecimal pendingPayout, BigDecimal paidPayout, String status) { }
