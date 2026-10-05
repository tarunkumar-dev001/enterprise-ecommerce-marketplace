package com.ecommerce.marketplace.dto;

import com.ecommerce.marketplace.entity.PayoutStatus;
import com.ecommerce.marketplace.entity.VendorLedger;
import com.ecommerce.marketplace.entity.OrderStatus;
import com.ecommerce.marketplace.entity.PaymentStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record VendorLedgerRecordResponse(
        Long id,
        Long orderId,
        Long subOrderId,
        Long vendorId,
        String vendorUsername,
        BigDecimal totalOrderAmount,
        BigDecimal platformCommissionFee,
        BigDecimal netVendorPayout,
        PayoutStatus payoutStatus,
        OrderStatus orderStatus,
        PaymentStatus paymentStatus,
        LocalDateTime createdAt,
        LocalDateTime paidAt) {

    public static VendorLedgerRecordResponse from(VendorLedger record) {
        return new VendorLedgerRecordResponse(
                record.getId(),
                record.getSubOrder().getOrder().getId(),
                record.getSubOrder().getId(),
                record.getVendor().getId(),
                record.getVendor().getUsername(),
                record.getTotalOrderAmount(),
                record.getPlatformCommissionFee(),
                record.getNetVendorPayout(),
                record.getPayoutStatus(),
                record.getSubOrder().getStatus(),
                record.getSubOrder().getOrder().getPaymentStatus(),
                record.getCreatedAt(),
                record.getPaidAt());
    }
}
