package com.ecommerce.marketplace.controller;

import com.ecommerce.marketplace.dto.WishlistItemRequest;
import com.ecommerce.marketplace.dto.WishlistResponse;
import com.ecommerce.marketplace.dto.CartResponse;
import com.ecommerce.marketplace.service.WishlistService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/wishlist")
public class WishlistController {

    private final WishlistService wishlistService;

    public WishlistController(WishlistService wishlistService) {
        this.wishlistService = wishlistService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('CUSTOMER','VENDOR')")
    public ResponseEntity<WishlistResponse> getWishlist(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(wishlistService.getWishlist(userDetails.getUsername()));
    }

    @PostMapping("/items")
    @PreAuthorize("hasAnyRole('CUSTOMER','VENDOR')")
    public ResponseEntity<WishlistResponse> addItem(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody WishlistItemRequest request) {
        return ResponseEntity.ok(wishlistService.addItem(userDetails.getUsername(), request));
    }

    @DeleteMapping("/items/{productId}")
    @PreAuthorize("hasAnyRole('CUSTOMER','VENDOR')")
    public ResponseEntity<WishlistResponse> removeItem(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long productId) {
        return ResponseEntity.ok(wishlistService.removeItem(userDetails.getUsername(), productId));
    }

    @PostMapping("/items/{productId}/move-to-cart")
    @PreAuthorize("hasAnyRole('CUSTOMER','VENDOR')")
    public ResponseEntity<CartResponse> moveItemToCart(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long productId) {
        return ResponseEntity.ok(wishlistService.moveItemToCart(userDetails.getUsername(), productId));
    }
}
