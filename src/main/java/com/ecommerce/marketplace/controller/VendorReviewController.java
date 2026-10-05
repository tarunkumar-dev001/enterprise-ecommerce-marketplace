package com.ecommerce.marketplace.controller;

import com.ecommerce.marketplace.dto.ProductReviewResponse;
import com.ecommerce.marketplace.service.ProductReviewService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/vendor/reviews")
public class VendorReviewController {
    private final ProductReviewService reviewService;

    public VendorReviewController(ProductReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @GetMapping
    @PreAuthorize("hasRole('VENDOR')")
    public List<ProductReviewResponse> getVendorReviews(@AuthenticationPrincipal UserDetails userDetails) {
        return reviewService.getVendorReviews(userDetails.getUsername());
    }
}
