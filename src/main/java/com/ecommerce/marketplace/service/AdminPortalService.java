package com.ecommerce.marketplace.service;

import com.ecommerce.marketplace.dto.*;
import com.ecommerce.marketplace.entity.*;
import com.ecommerce.marketplace.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class AdminPortalService {
    private static final BigDecimal ZERO = new BigDecimal("0.00");
    private final UserRepository users;
    private final ProductRepository products;
    private final OrderRepository orders;
    private final ProductReviewRepository reviews;
    private final VendorLedgerRepository ledger;

    public AdminPortalService(UserRepository users, ProductRepository products, OrderRepository orders,
            ProductReviewRepository reviews, VendorLedgerRepository ledger) {
        this.users = users; this.products = products; this.orders = orders; this.reviews = reviews; this.ledger = ledger;
    }

    public List<AdminUserResponse> users(String role) {
        return users.findAll().stream()
                .filter(user -> role == null || role.isBlank() || user.getRoles().contains(normalizeRole(role)))
                .sorted(Comparator.comparing(User::getUsername, String.CASE_INSENSITIVE_ORDER))
                .map(AdminUserResponse::from).toList();
    }

    public List<AdminProductResponse> products() {
        return products.findAll().stream().map(AdminProductResponse::from).toList();
    }

    public List<OrderResponse> orders() {
        return orders.findAll().stream().sorted(Comparator.comparing(Order::getPlacedAt).reversed())
                .map(OrderResponse::from).toList();
    }

    public List<ProductReviewResponse> reviews() {
        return reviews.findAll().stream().sorted(Comparator.comparing(ProductReview::getCreatedAt).reversed())
                .map(ProductReviewResponse::from).toList();
    }

    public List<AdminVendorResponse> vendors() {
        List<User> vendorUsers = users.findAll().stream().filter(user -> user.getRoles().contains("ROLE_VENDOR")).toList();
        List<VendorLedger> ledgers = ledger.findAllByOrderByCreatedAtDesc();
        List<Order> allOrders = orders.findAll();
        List<Product> allProducts = products.findAll();
        return vendorUsers.stream().map(vendor -> {
            List<VendorLedger> vendorRecords = ledgers.stream().filter(row -> row.getVendor().getId().equals(vendor.getId())).toList();
            long productCount = allProducts.stream().filter(product -> product.getVendor().getId().equals(vendor.getId())).count();
            long orderCount = allOrders.stream().filter(order -> order.getSubOrders().stream()
                    .anyMatch(sub -> sub.getVendor().getId().equals(vendor.getId()))).count();
            return new AdminVendorResponse(vendor.getId(), vendor.getUsername(), vendor.getEmail(), productCount, orderCount,
                    sum(vendorRecords, VendorLedger::getTotalOrderAmount),
                    sum(vendorRecords.stream().filter(row -> row.getPayoutStatus() == PayoutStatus.PENDING).toList(), VendorLedger::getNetVendorPayout),
                    sum(vendorRecords.stream().filter(row -> row.getPayoutStatus() == PayoutStatus.PAID).toList(), VendorLedger::getNetVendorPayout), "NOT_TRACKED");
        }).sorted(Comparator.comparing(AdminVendorResponse::grossRevenue).reversed()).toList();
    }

    public AdminDashboardResponse dashboard() {
        List<User> allUsers = users.findAll();
        List<Product> allProducts = products.findAll();
        List<Order> allOrders = orders.findAll();
        List<VendorLedger> allLedger = ledger.findAllByOrderByCreatedAtDesc();
        List<ProductReview> allReviews = reviews.findAll();
        Map<String, Long> statuses = Arrays.stream(OrderStatus.values()).collect(Collectors.toMap(Enum::name,
                status -> allOrders.stream().filter(order -> order.getStatus() == status).count(), (a,b) -> a, LinkedHashMap::new));
        Map<Long, Long> sold = allOrders.stream().filter(order -> order.getStatus() != OrderStatus.CANCELLED)
                .flatMap(order -> order.getItems().stream())
                .collect(Collectors.groupingBy(item -> item.getProduct().getId(), Collectors.summingLong(OrderItem::getQuantity)));
        List<AdminProductResponse> topProducts = allProducts.stream().sorted(Comparator.comparingLong((Product p) -> sold.getOrDefault(p.getId(), 0L)).reversed())
                .limit(5).map(product -> withUnitsSold(product, sold.getOrDefault(product.getId(), 0L))).toList();
        List<OrderResponse> recentOrders = allOrders.stream().sorted(Comparator.comparing(Order::getPlacedAt).reversed()).limit(8).map(OrderResponse::from).toList();
        List<ProductReviewResponse> recentReviews = allReviews.stream().sorted(Comparator.comparing(ProductReview::getCreatedAt).reversed()).limit(5).map(ProductReviewResponse::from).toList();
        List<AdminVendorResponse> vendors = vendors();
        long vendorCount = allUsers.stream().filter(user -> user.getRoles().contains("ROLE_VENDOR")).count();
        long customerCount = allUsers.stream().filter(user -> user.getRoles().contains("ROLE_USER") || user.getRoles().contains("ROLE_CUSTOMER")).count();
        return new AdminDashboardResponse(allUsers.size(), vendorCount, customerCount, allProducts.size(), allOrders.size(),
                allOrders.stream().filter(order -> order.getPaymentStatus() == PaymentStatus.PAID)
                        .map(Order::getTotalAmount).filter(Objects::nonNull).reduce(ZERO, BigDecimal::add),
                sum(allLedger.stream().filter(row -> row.getPayoutStatus() == PayoutStatus.PENDING).toList(), VendorLedger::getNetVendorPayout),
                sum(allLedger, VendorLedger::getPlatformCommissionFee), statuses, recentOrders, topProducts,
                vendors.stream().limit(5).toList(), recentReviews);
    }

    private static String normalizeRole(String role) {
        String normalized = role.trim().toUpperCase(Locale.ROOT);
        return normalized.startsWith("ROLE_") ? normalized : "ROLE_" + normalized;
    }

    private static BigDecimal sum(List<VendorLedger> rows, Function<VendorLedger, BigDecimal> getter) {
        return rows.stream().map(getter).filter(Objects::nonNull).reduce(ZERO, BigDecimal::add);
    }

    private static AdminProductResponse withUnitsSold(Product product, long unitsSold) {
        AdminProductResponse row = AdminProductResponse.from(product);
        return new AdminProductResponse(row.id(), row.name(), row.category(), row.imageUrl(), row.vendorId(), row.vendorUsername(),
                row.price(), row.stock(), row.averageRating(), row.reviewCount(), unitsSold, row.status(),
                row.moderationStatus(), row.moderationReason(), row.stockStatus());
    }
}
