package com.ecommerce.marketplace.controller;

import com.ecommerce.marketplace.dto.VendorLedgerSummaryResponse;
import com.ecommerce.marketplace.service.VendorLedgerService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/vendor/ledger")
public class VendorLedgerController {

    private final VendorLedgerService ledgerService;

    public VendorLedgerController(VendorLedgerService ledgerService) {
        this.ledgerService = ledgerService;
    }

    @GetMapping
    @PreAuthorize("hasRole('VENDOR')")
    public ResponseEntity<VendorLedgerSummaryResponse> getLedger(
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(ledgerService.getVendorLedger(userDetails.getUsername()));
    }
}
