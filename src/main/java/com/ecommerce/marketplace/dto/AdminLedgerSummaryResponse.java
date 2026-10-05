package com.ecommerce.marketplace.dto;

import java.math.BigDecimal;
import java.util.List;

public record AdminLedgerSummaryResponse(
        BigDecimal grossSales,
        BigDecimal platformCommission,
        BigDecimal vendorNetEarnings,
        BigDecimal pendingPayouts,
        BigDecimal paidOut,
        List<VendorLedgerRecordResponse> records) {
}
