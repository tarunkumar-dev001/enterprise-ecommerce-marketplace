package com.ecommerce.marketplace.controller;

import com.ecommerce.marketplace.dto.AdminLedgerSummaryResponse;
import com.ecommerce.marketplace.dto.VendorLedgerSummaryResponse;
import com.ecommerce.marketplace.service.VendorLedgerService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
public class AdminLedgerController {

    private final VendorLedgerService ledgerService;

    public AdminLedgerController(VendorLedgerService ledgerService) {
        this.ledgerService = ledgerService;
    }

    @GetMapping("/ledger/all")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AdminLedgerSummaryResponse> getAllLedgerRecords() {
        return ResponseEntity.ok(ledgerService.getAllLedgerRecords());
    }

    @PostMapping("/payouts/{vendorId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<VendorLedgerSummaryResponse> payPendingPayouts(@PathVariable Long vendorId) {
        return ResponseEntity.ok(ledgerService.payPendingRecords(vendorId));
    }
}
