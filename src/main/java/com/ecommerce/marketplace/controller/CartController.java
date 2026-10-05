package com.ecommerce.marketplace.controller;

import com.ecommerce.marketplace.dto.CartItemRequest;
import com.ecommerce.marketplace.dto.CartResponse;
import com.ecommerce.marketplace.dto.ApplyCouponRequest;
import com.ecommerce.marketplace.dto.UpdateCartItemRequest;
import com.ecommerce.marketplace.service.CartService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class CartController {

    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    @GetMapping("/cart")
    @PreAuthorize("hasAnyRole('USER','CUSTOMER','VENDOR')")
    public ResponseEntity<CartResponse> getCart(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(cartService.getCart(userDetails.getUsername()));
    }

    @PostMapping("/cart/items")
    @PreAuthorize("hasAnyRole('USER','CUSTOMER','VENDOR')")
    public ResponseEntity<CartResponse> addItemToCart(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody CartItemRequest request) {
        return ResponseEntity.ok(cartService.addItemToCart(userDetails.getUsername(), request));
    }

    @PostMapping("/cart/coupon")
    @PreAuthorize("hasAnyRole('USER','CUSTOMER','VENDOR')")
    public ResponseEntity<CartResponse> applyCoupon(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody ApplyCouponRequest request) {
        return ResponseEntity.ok(cartService.applyCoupon(userDetails.getUsername(), request));
    }

    @PutMapping("/cart/items/{productId}")
    @PreAuthorize("hasAnyRole('USER','CUSTOMER','VENDOR')")
    public ResponseEntity<CartResponse> updateItemQuantity(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long productId,
            @Valid @RequestBody UpdateCartItemRequest request) {
        return ResponseEntity.ok(cartService.updateItemQuantity(userDetails.getUsername(), productId, request));
    }

    @DeleteMapping("/cart/items/{productId}")
    @PreAuthorize("hasAnyRole('USER','CUSTOMER','VENDOR')")
    public ResponseEntity<CartResponse> removeItem(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long productId) {
        return ResponseEntity.ok(cartService.removeItem(userDetails.getUsername(), productId));
    }

    @DeleteMapping("/cart/coupon")
    @PreAuthorize("hasAnyRole('USER','CUSTOMER','VENDOR')")
    public ResponseEntity<CartResponse> removeCoupon(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(cartService.removeCoupon(userDetails.getUsername()));
    }
}
