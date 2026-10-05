package com.ecommerce.marketplace.dto;

import com.ecommerce.marketplace.entity.SubOrder;
import com.ecommerce.marketplace.entity.OrderStatus;

import java.math.BigDecimal;
import java.util.List;

public record SubOrderResponse(
        Long id,
        Long vendorId,
        String vendorUsername,
        BigDecimal totalAmount,
        OrderStatus status,
        List<OrderItemResponse> items) {

    public static SubOrderResponse from(SubOrder subOrder) {
        return new SubOrderResponse(
                subOrder.getId(),
                subOrder.getVendor().getId(),
                subOrder.getVendor().getUsername(),
                subOrder.getTotalAmount(),
                subOrder.getStatus(),
                subOrder.getItems().stream().map(OrderItemResponse::from).toList());
    }
}
