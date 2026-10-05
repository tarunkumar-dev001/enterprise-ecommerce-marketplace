package com.ecommerce.marketplace.service;

import com.ecommerce.marketplace.dto.VendorAnalyticsResponse;
import com.ecommerce.marketplace.dto.VendorBestSellingProductResponse;
import com.ecommerce.marketplace.entity.OrderStatus;
import com.ecommerce.marketplace.entity.PaymentStatus;
import com.ecommerce.marketplace.entity.User;
import com.ecommerce.marketplace.exception.ResourceNotFoundException;
import com.ecommerce.marketplace.repository.OrderItemRepository;
import com.ecommerce.marketplace.repository.UserRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class VendorAnalyticsService {

    private final OrderItemRepository orderItemRepository;
    private final UserRepository userRepository;

    public VendorAnalyticsService(OrderItemRepository orderItemRepository, UserRepository userRepository) {
        this.orderItemRepository = orderItemRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public VendorAnalyticsResponse getAnalytics(String username, int limit) {
        if (limit < 1 || limit > 100) {
            throw new IllegalArgumentException("Product ranking limit must be between 1 and 100");
        }
        User vendor = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        if (!vendor.getRoles().contains("ROLE_VENDOR")) {
            throw new AccessDeniedException("Only vendors can view vendor analytics");
        }

        OrderItemRepository.VendorSalesMetricsProjection metrics = orderItemRepository.getVendorSalesMetrics(
                vendor.getId(), PaymentStatus.PAID, OrderStatus.CANCELLED);
        List<VendorBestSellingProductResponse> bestSellingProducts = orderItemRepository.findBestSellingProducts(
                vendor.getId(), PaymentStatus.PAID, OrderStatus.CANCELLED, PageRequest.of(0, limit));

        return new VendorAnalyticsResponse(
                metrics.getTotalSalesRevenue() == null ? BigDecimal.ZERO : metrics.getTotalSalesRevenue(),
                metrics.getOrderCount() == null ? 0 : metrics.getOrderCount(),
                metrics.getUnitsSold() == null ? 0 : metrics.getUnitsSold(),
                bestSellingProducts);
    }
}
