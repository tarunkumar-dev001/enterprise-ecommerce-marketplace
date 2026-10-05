package com.ecommerce.marketplace.controller;

import com.ecommerce.marketplace.dto.ProductRequest;
import com.ecommerce.marketplace.dto.ProductPageResponse;
import com.ecommerce.marketplace.dto.ProductDetailsResponse;
import com.ecommerce.marketplace.dto.ProductResponse;
import com.ecommerce.marketplace.service.ProductService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.math.BigDecimal;

@RestController
@RequestMapping("/api")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping("/products")
    @PreAuthorize("hasAnyRole('USER','CUSTOMER','VENDOR')")
    public ResponseEntity<ProductPageResponse> listProducts(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "desc") String direction) {
        return ResponseEntity.ok(productService.browseProducts(
            search, category, minPrice, maxPrice, page, size, sortBy, direction));
    }

    @GetMapping("/products/{id}")
    @PreAuthorize("hasAnyRole('USER','CUSTOMER','VENDOR')")
    public ResponseEntity<ProductDetailsResponse> getProductDetails(@PathVariable Long id) {
        return ResponseEntity.ok(productService.getProductDetails(id));
    }

    @GetMapping("/vendor/products")
    @PreAuthorize("hasRole('VENDOR')")
    public ResponseEntity<List<ProductResponse>> listVendorProducts(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(productService.listProducts(userDetails.getUsername()));
    }

    @PostMapping("/products")
    @PreAuthorize("hasRole('VENDOR')")
    public ResponseEntity<ProductResponse> createProduct(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody ProductRequest request) {
        return ResponseEntity.ok(productService.createProduct(userDetails.getUsername(), request));
    }

    @PutMapping("/products/{id}")
    @PreAuthorize("hasRole('VENDOR')")
    public ResponseEntity<ProductResponse> updateProduct(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id,
            @Valid @RequestBody ProductRequest request) {
        return ResponseEntity.ok(productService.updateProduct(userDetails.getUsername(), id, request));
    }
}
