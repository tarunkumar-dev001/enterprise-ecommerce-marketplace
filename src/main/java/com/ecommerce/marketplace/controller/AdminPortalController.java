package com.ecommerce.marketplace.controller;

import com.ecommerce.marketplace.dto.*;
import com.ecommerce.marketplace.service.AdminPortalService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminPortalController {
    private final AdminPortalService service;
    public AdminPortalController(AdminPortalService service) { this.service = service; }

    @GetMapping("/dashboard")
    public AdminDashboardResponse dashboard() { return service.dashboard(); }

    @GetMapping("/users")
    public List<AdminUserResponse> users(@RequestParam(required = false) String role) { return service.users(role); }

    @GetMapping("/vendors")
    public List<AdminVendorResponse> vendors() { return service.vendors(); }

    @GetMapping("/products")
    public List<AdminProductResponse> products() { return service.products(); }

    @GetMapping("/orders")
    public List<OrderResponse> orders() { return service.orders(); }

    @GetMapping("/reviews")
    public List<ProductReviewResponse> reviews() { return service.reviews(); }
}
