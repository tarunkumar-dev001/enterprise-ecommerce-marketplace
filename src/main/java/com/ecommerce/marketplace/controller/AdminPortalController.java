package com.ecommerce.marketplace.controller;

import com.ecommerce.marketplace.dto.*;
import com.ecommerce.marketplace.service.AdminPortalService;
import com.ecommerce.marketplace.service.ProductModerationService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminPortalController {
    private final AdminPortalService service;
    private final ProductModerationService moderationService;

    public AdminPortalController(AdminPortalService service, ProductModerationService moderationService) {
        this.service = service;
        this.moderationService = moderationService;
    }

    @GetMapping("/dashboard")
    public AdminDashboardResponse dashboard() { return service.dashboard(); }

    @GetMapping("/users")
    public List<AdminUserResponse> users(@RequestParam(required = false) String role) { return service.users(role); }

    @GetMapping("/vendors")
    public List<AdminVendorResponse> vendors() { return service.vendors(); }

    @GetMapping("/products")
    public List<AdminProductResponse> products() { return service.products(); }

    @PatchMapping("/products/{productId}/approve")
    public ResponseEntity<AdminProductResponse> approveProduct(@PathVariable Long productId, @AuthenticationPrincipal UserDetails administrator) {
        return ResponseEntity.ok(moderationService.approve(productId, administrator.getUsername()));
    }

    @PatchMapping("/products/{productId}/reject")
    public ResponseEntity<AdminProductResponse> rejectProduct(
            @PathVariable Long productId,
            @Valid @RequestBody ProductModerationReasonRequest request,
            @AuthenticationPrincipal UserDetails administrator) {
        return ResponseEntity.ok(moderationService.reject(productId, request.reason(), administrator.getUsername()));
    }

    @PatchMapping("/products/{productId}/suspend")
    public ResponseEntity<AdminProductResponse> suspendProduct(
            @PathVariable Long productId,
            @Valid @RequestBody ProductModerationReasonRequest request,
            @AuthenticationPrincipal UserDetails administrator) {
        return ResponseEntity.ok(moderationService.suspend(productId, request.reason(), administrator.getUsername()));
    }

    @PatchMapping("/products/{productId}/reinstate")
    public ResponseEntity<AdminProductResponse> reinstateProduct(
            @PathVariable Long productId,
            @Valid @RequestBody ProductModerationReasonRequest request,
            @AuthenticationPrincipal UserDetails administrator) {
        return ResponseEntity.ok(moderationService.reinstate(productId, request.reason(), administrator.getUsername()));
    }

    @GetMapping("/orders")
    public List<OrderResponse> orders() { return service.orders(); }

    @GetMapping("/reviews")
    public List<ProductReviewResponse> reviews() { return service.reviews(); }
}
