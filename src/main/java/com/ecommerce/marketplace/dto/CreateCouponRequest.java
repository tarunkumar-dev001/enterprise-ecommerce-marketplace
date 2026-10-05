package com.ecommerce.marketplace.dto;

import com.ecommerce.marketplace.entity.DiscountType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record CreateCouponRequest(
        @NotBlank(message = "Coupon code is required")
        @Size(min = 3, max = 32, message = "Coupon code must be between 3 and 32 characters")
        @Pattern(regexp = "[A-Za-z0-9_-]+", message = "Coupon code may contain only letters, numbers, '_' and '-'")
        String code,
        @NotNull(message = "Discount type is required") DiscountType discountType,
        @NotNull(message = "Discount value is required")
        @DecimalMin(value = "0.01", message = "Discount value must be greater than zero")
        BigDecimal discountValue,
        @Future(message = "Coupon expiration must be in the future") LocalDateTime expiresAt,
        @Min(value = 1, message = "Usage limit must be at least 1") Integer usageLimit) {
}