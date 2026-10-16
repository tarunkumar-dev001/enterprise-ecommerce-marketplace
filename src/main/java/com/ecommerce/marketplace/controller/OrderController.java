package com.ecommerce.marketplace.controller;

import com.ecommerce.marketplace.dto.CheckoutRequest;
import com.ecommerce.marketplace.dto.OrderResponse;
import com.ecommerce.marketplace.dto.PaymentRequest;
import com.ecommerce.marketplace.dto.PaymentResponse;
import com.ecommerce.marketplace.service.InvoiceService;
import com.ecommerce.marketplace.service.OrderService;
import com.ecommerce.marketplace.service.PaymentService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ContentDisposition;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
public class OrderController {

    private final OrderService orderService;
    private final PaymentService paymentService;
    private final InvoiceService invoiceService;

    public OrderController(OrderService orderService, PaymentService paymentService, InvoiceService invoiceService) {
        this.orderService = orderService;
        this.paymentService = paymentService;
        this.invoiceService = invoiceService;
    }

    @PostMapping("/orders")
    @PreAuthorize("hasAnyRole('CUSTOMER','VENDOR')")
    public ResponseEntity<OrderResponse> placeOrder(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(orderService.placeOrder(userDetails.getUsername()));
    }

    @PostMapping("/orders/checkout")
    @PreAuthorize("hasAnyRole('CUSTOMER','VENDOR')")
    public ResponseEntity<OrderResponse> checkout(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody CheckoutRequest request) {
        return ResponseEntity.ok(orderService.checkout(userDetails.getUsername(), request));
    }

    @GetMapping("/orders")
    @PreAuthorize("hasAnyRole('CUSTOMER','VENDOR')")
    public ResponseEntity<List<OrderResponse>> getOrders(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(orderService.getCustomerOrders(userDetails.getUsername()));
    }

    @GetMapping("/orders/{id}")
    @PreAuthorize("hasAnyRole('CUSTOMER','VENDOR')")
    public ResponseEntity<OrderResponse> getOrder(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id) {
        return ResponseEntity.ok(orderService.getCustomerOrder(userDetails.getUsername(), id));
    }

    @PostMapping("/orders/{id}/cancel")
    @PreAuthorize("hasAnyRole('CUSTOMER','VENDOR')")
    public ResponseEntity<OrderResponse> cancelOrder(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id) {
        return ResponseEntity.ok(orderService.cancelCustomerOrder(userDetails.getUsername(), id));
    }

    @PostMapping("/orders/{id}/payments")
    @PreAuthorize("hasAnyRole('CUSTOMER','VENDOR')")
    public ResponseEntity<PaymentResponse> processPayment(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id,
            @Valid @RequestBody PaymentRequest request) {
        return ResponseEntity.ok(paymentService.processPayment(userDetails.getUsername(), id, request));
    }

    @GetMapping("/orders/{id}/invoice")
    @PreAuthorize("hasAnyRole('CUSTOMER','VENDOR')")
    public ResponseEntity<byte[]> downloadInvoice(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id) {
        byte[] invoice = invoiceService.generateInvoice(userDetails.getUsername(), id);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename("invoice-" + id + ".pdf").build().toString())
                .body(invoice);
    }
}
