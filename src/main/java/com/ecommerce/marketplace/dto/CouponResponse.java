package com.ecommerce.marketplace.dto;

import com.ecommerce.marketplace.entity.Coupon;
import com.ecommerce.marketplace.entity.DiscountType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record CouponResponse(
        Long id,
        String code,
        DiscountType discountType,
        BigDecimal discountValue,
        String vendorUsername,
        LocalDateTime expiresAt,
        Integer usageLimit,
        int usageCount,
        boolean active) {

    public static CouponResponse from(Coupon coupon) {
        return new CouponResponse(
                coupon.getId(),
                coupon.getCode(),
                coupon.getDiscountType(),
                coupon.getDiscountValue(),
                coupon.getVendor() == null ? null : coupon.getVendor().getUsername(),
                coupon.getExpiresAt(),
                coupon.getUsageLimit(),
                coupon.getUsageCount(),
                coupon.isActive());
    }
}