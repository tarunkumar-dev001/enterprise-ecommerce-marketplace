package com.ecommerce.marketplace.service;

import com.ecommerce.marketplace.dto.PaymentRequest;
import com.ecommerce.marketplace.dto.PaymentResponse;
import com.ecommerce.marketplace.entity.Order;
import com.ecommerce.marketplace.entity.OrderStatus;
import com.ecommerce.marketplace.entity.PaymentStatus;
import com.ecommerce.marketplace.entity.User;
import com.ecommerce.marketplace.exception.BusinessException;
import com.ecommerce.marketplace.exception.ResourceNotFoundException;
import com.ecommerce.marketplace.repository.OrderRepository;
import com.ecommerce.marketplace.repository.UserRepository;
import com.ecommerce.marketplace.service.payment.PaymentGateway;
import com.ecommerce.marketplace.service.payment.PaymentResult;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PaymentService {

    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final PaymentGateway paymentGateway;

    public PaymentService(
            OrderRepository orderRepository,
            UserRepository userRepository,
            PaymentGateway paymentGateway) {
        this.orderRepository = orderRepository;
        this.userRepository = userRepository;
        this.paymentGateway = paymentGateway;
    }

    @Transactional
    public PaymentResponse processPayment(String username, Long orderId, PaymentRequest request) {
        User customer = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        Order order = orderRepository.findByIdAndUserForPayment(orderId, customer)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        if (order.getStatus() == OrderStatus.CANCELLED) {
            throw new BusinessException("Cancelled orders cannot be paid", HttpStatus.CONFLICT);
        }
        if (order.getPaymentStatus() == PaymentStatus.PAID) {
            throw new BusinessException("Order has already been paid", HttpStatus.CONFLICT);
        }

        PaymentResult result = paymentGateway.charge(
                order.getId(), order.getTotalAmount(), Boolean.TRUE.equals(request.simulateFailure()));
        order.setPaymentStatus(result.status());
        order.setPaymentReference(result.transactionReference());
        orderRepository.save(order);

        String message = result.status() == PaymentStatus.PAID
                ? "Demo payment completed successfully; no real money was charged"
                : "Demo payment failed; you may retry the test transaction";
        return new PaymentResponse(
                order.getId(),
                order.getTotalAmount(),
                result.status(),
                result.transactionReference(),
                message);
    }
}
