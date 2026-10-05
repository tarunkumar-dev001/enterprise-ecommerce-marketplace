package com.ecommerce.marketplace.controller;

import com.ecommerce.marketplace.dto.VendorAnalyticsResponse;
import com.ecommerce.marketplace.service.VendorAnalyticsService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/vendor/analytics")
public class VendorAnalyticsController {

    private final VendorAnalyticsService vendorAnalyticsService;

    public VendorAnalyticsController(VendorAnalyticsService vendorAnalyticsService) {
        this.vendorAnalyticsService = vendorAnalyticsService;
    }

    @GetMapping
    @PreAuthorize("hasRole('VENDOR')")
    public ResponseEntity<VendorAnalyticsResponse> getAnalytics(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(defaultValue = "10") int limit) {
        return ResponseEntity.ok(vendorAnalyticsService.getAnalytics(userDetails.getUsername(), limit));
    }
}
