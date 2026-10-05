package com.ecommerce.marketplace.dto;

import java.math.BigDecimal;
import java.util.List;

public record VendorLedgerSummaryResponse(
        Long vendorId,
        String vendorUsername,
        BigDecimal grossSales,
        BigDecimal totalCommission,
        BigDecimal netEarnings,
        BigDecimal pendingPayouts,
        BigDecimal paidOut,
        List<VendorLedgerRecordResponse> records) {
}
