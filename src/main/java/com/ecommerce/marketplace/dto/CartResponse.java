package com.ecommerce.marketplace.dto;

import com.ecommerce.marketplace.entity.Cart;
import com.ecommerce.marketplace.entity.CartItem;
import com.ecommerce.marketplace.entity.ProductModerationStatus;

import java.math.BigDecimal;
import java.util.List;

public record CartResponse(
        Long id,
        List<CartItemResponse> items,
        BigDecimal subtotal,
        BigDecimal discountAmount,
        String couponCode,
        BigDecimal totalAmount) {

    public static CartResponse from(Cart cart) {
        return from(cart, BigDecimal.ZERO);
    }

    public static CartResponse from(Cart cart, BigDecimal discountAmount) {
        List<CartItemResponse> items = cart.getItems().stream()
                // Retain unavailable rows in storage so customers can remove them, but never
                // serialize a moderated product or include its price in totals/discounts.
                .filter(CartResponse::isAvailable)
                .map(CartItemResponse::from)
                .toList();

        BigDecimal subtotal = items.stream()
                .map(CartItemResponse::subtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal appliedDiscount = discountAmount.min(subtotal);
        BigDecimal totalAmount = subtotal.subtract(appliedDiscount);

        return new CartResponse(
                cart.getId(),
                items,
                subtotal,
                appliedDiscount,
                cart.getCoupon() == null ? null : cart.getCoupon().getCode(),
                totalAmount);
    }

    public static List<CartItem> availableItems(Cart cart) {
        return cart.getItems().stream().filter(CartResponse::isAvailable).toList();
    }

    private static boolean isAvailable(CartItem item) {
        return item.getProduct().getModerationStatus() == ProductModerationStatus.APPROVED;
    }
}
