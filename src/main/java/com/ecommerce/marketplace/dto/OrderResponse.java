package com.ecommerce.marketplace.dto;

import com.ecommerce.marketplace.entity.Order;
import com.ecommerce.marketplace.entity.OrderStatus;
import com.ecommerce.marketplace.entity.PaymentStatus;
import com.ecommerce.marketplace.entity.OrderItem;
import com.ecommerce.marketplace.entity.SubOrder;
import com.ecommerce.marketplace.entity.User;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

public record OrderResponse(
        Long id,
        Long userId,
        String username,
        BigDecimal totalAmount,
        BigDecimal subtotalAmount,
        BigDecimal discountAmount,
        String couponCode,
        OrderStatus status,
        PaymentStatus paymentStatus,
        String paymentReference,
        LocalDateTime placedAt,
        ShippingAddress shippingAddress,
        List<OrderItemResponse> items,
        List<SubOrderResponse> subOrders) {
    public static OrderResponse from(Order order) {
        List<OrderItemResponse> items = order.getItems().stream()
                .map(OrderItemResponse::from)
                .toList();

        return new OrderResponse(
                order.getId(),
                order.getUser().getId(),
                order.getUser().getUsername(),
                order.getTotalAmount(),
                order.getSubtotalAmount(),
                order.getDiscountAmount(),
                order.getCouponCode(),
                order.getStatus(),
                order.getPaymentStatus(),
                order.getPaymentReference(),
                order.getPlacedAt(),
                ShippingAddress.from(order.getShippingAddress()),
                items,
                order.getSubOrders().stream().map(SubOrderResponse::from).toList());
    }

    public static OrderResponse forVendor(Order order, User vendor) {
        SubOrder subOrder = order.getSubOrders().stream()
                .filter(candidate -> Objects.equals(candidate.getVendor().getId(), vendor.getId()))
                .findFirst()
                .orElse(null);

        List<OrderItem> vendorItems = subOrder == null
                ? order.getItems().stream()
                        .filter(item -> Objects.equals(item.getProduct().getVendor().getId(), vendor.getId()))
                        .toList()
                : subOrder.getItems();
        List<OrderItemResponse> items = vendorItems.stream().map(OrderItemResponse::from).toList();
        BigDecimal vendorTotal = vendorItems.stream()
                .map(item -> item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        OrderStatus vendorStatus = subOrder == null ? order.getStatus() : subOrder.getStatus();
        SubOrderResponse vendorSummary = new SubOrderResponse(
                subOrder == null ? null : subOrder.getId(),
                vendor.getId(),
                vendor.getUsername(),
                vendorTotal,
                vendorStatus,
                items);

        return new OrderResponse(
                order.getId(),
                order.getUser().getId(),
                order.getUser().getUsername(),
                vendorTotal,
                vendorTotal,
                BigDecimal.ZERO,
                order.getCouponCode(),
                vendorStatus,
                order.getPaymentStatus(),
                order.getPaymentReference(),
                order.getPlacedAt(),
                ShippingAddress.from(order.getShippingAddress()),
                items,
                List.of(vendorSummary));
    }
}
