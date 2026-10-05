package com.ecommerce.marketplace.service;

import com.ecommerce.marketplace.dto.CreateCouponRequest;
import com.ecommerce.marketplace.dto.CouponResponse;
import com.ecommerce.marketplace.entity.CartItem;
import com.ecommerce.marketplace.entity.Coupon;
import com.ecommerce.marketplace.entity.DiscountType;
import com.ecommerce.marketplace.entity.User;
import com.ecommerce.marketplace.exception.BusinessException;
import com.ecommerce.marketplace.exception.ResourceNotFoundException;
import com.ecommerce.marketplace.repository.CouponRepository;
import com.ecommerce.marketplace.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

@Service
public class CouponService {

    private final CouponRepository couponRepository;
    private final UserRepository userRepository;

    public CouponService(CouponRepository couponRepository, UserRepository userRepository) {
        this.couponRepository = couponRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public CouponResponse createCoupon(String username, CreateCouponRequest request) {
        User creator = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        boolean isAdmin = creator.getRoles().contains("ROLE_ADMIN");
        boolean isVendor = creator.getRoles().contains("ROLE_VENDOR");
        if (!isAdmin && !isVendor) {
            throw new org.springframework.security.access.AccessDeniedException("Only vendors and admins can create coupons");
        }
        if (request.discountType() == DiscountType.PERCENTAGE
                && request.discountValue().compareTo(BigDecimal.valueOf(100)) > 0) {
            throw new IllegalArgumentException("Percentage discount cannot exceed 100");
        }

        String code = normalizeCode(request.code());
        if (couponRepository.existsByCodeIgnoreCase(code)) {
            throw new BusinessException("Coupon code already exists", HttpStatus.CONFLICT);
        }

        Coupon coupon = new Coupon();
        coupon.setCode(code);
        coupon.setDiscountType(request.discountType());
        coupon.setDiscountValue(request.discountValue().setScale(2, RoundingMode.HALF_UP));
        coupon.setExpiresAt(request.expiresAt());
        coupon.setUsageLimit(request.usageLimit());
        coupon.setVendor(isAdmin ? null : creator);
        return CouponResponse.from(couponRepository.save(coupon));
    }

    @Transactional(readOnly = true)
    public Coupon findUsableCoupon(String code) {
        String normalizedCode = normalizeCode(code);
        Coupon coupon = couponRepository.findByCodeIgnoreCase(normalizedCode)
                .orElseThrow(() -> new ResourceNotFoundException("Coupon not found"));
        validateUsable(coupon);
        return coupon;
    }

    @Transactional
    public BigDecimal consumeCoupon(Long couponId, List<CartItem> items) {
        Coupon coupon = couponRepository.findByIdForUpdate(couponId)
                .orElseThrow(() -> new ResourceNotFoundException("Coupon not found"));
        validateUsable(coupon);
        BigDecimal discount = calculateDiscount(coupon, items);
        coupon.setUsageCount(coupon.getUsageCount() + 1);
        couponRepository.save(coupon);
        return discount;
    }

    public BigDecimal calculateDiscount(Coupon coupon, List<CartItem> items) {
        BigDecimal eligibleSubtotal = items.stream()
                .filter(item -> coupon.getVendor() == null
                        || coupon.getVendor().getId().equals(item.getProduct().getVendor().getId()))
                .map(item -> item.getProduct().getPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (eligibleSubtotal.signum() == 0) {
            throw new IllegalArgumentException("Coupon does not apply to any items in the cart");
        }

        BigDecimal discount = switch (coupon.getDiscountType()) {
            case PERCENTAGE -> eligibleSubtotal.multiply(coupon.getDiscountValue())
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            case FIXED -> coupon.getDiscountValue().min(eligibleSubtotal);
        };
        return discount.setScale(2, RoundingMode.HALF_UP);
    }

    private void validateUsable(Coupon coupon) {
        if (!coupon.isActive()) {
            throw new BusinessException("Coupon is inactive", HttpStatus.CONFLICT);
        }
        if (coupon.getExpiresAt() != null && !coupon.getExpiresAt().isAfter(LocalDateTime.now())) {
            throw new BusinessException("Coupon has expired", HttpStatus.CONFLICT);
        }
        if (coupon.getUsageLimit() != null && coupon.getUsageCount() >= coupon.getUsageLimit()) {
            throw new BusinessException("Coupon usage limit has been reached", HttpStatus.CONFLICT);
        }
    }

    private String normalizeCode(String code) {
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("Coupon code is required");
        }
        return code.trim().toUpperCase(Locale.ROOT);
    }
}
