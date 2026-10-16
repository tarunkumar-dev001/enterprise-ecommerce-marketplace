package com.ecommerce.marketplace.controller;

import com.ecommerce.marketplace.dto.ProductReviewRequest;
import com.ecommerce.marketplace.dto.ProductReviewResponse;
import com.ecommerce.marketplace.service.ProductReviewService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/products/{productId}/reviews")
public class ProductReviewController {

    private final ProductReviewService productReviewService;

    public ProductReviewController(ProductReviewService productReviewService) {
        this.productReviewService = productReviewService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('CUSTOMER','VENDOR')")
    public ResponseEntity<ProductReviewResponse> createReview(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long productId,
            @Valid @RequestBody ProductReviewRequest request) {
        return ResponseEntity.ok(productReviewService.createReview(userDetails.getUsername(), productId, request));
    }

    @GetMapping
    public ResponseEntity<List<ProductReviewResponse>> getProductReviews(@PathVariable Long productId) {
        return ResponseEntity.ok(productReviewService.getProductReviews(productId));
    }
}
