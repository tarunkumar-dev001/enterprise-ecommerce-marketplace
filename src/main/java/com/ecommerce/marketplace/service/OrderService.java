package com.ecommerce.marketplace.service;

import com.ecommerce.marketplace.dto.OrderResponse;
import com.ecommerce.marketplace.dto.CheckoutItemRequest;
import com.ecommerce.marketplace.dto.CheckoutRequest;
import com.ecommerce.marketplace.dto.UpdateOrderStatusRequest;
import com.ecommerce.marketplace.entity.Cart;
import com.ecommerce.marketplace.entity.Order;
import com.ecommerce.marketplace.entity.OrderItem;
import com.ecommerce.marketplace.entity.OrderStatus;
import com.ecommerce.marketplace.entity.Product;
import com.ecommerce.marketplace.entity.SubOrder;
import com.ecommerce.marketplace.entity.User;
import com.ecommerce.marketplace.event.OrderPlacedEvent;
import com.ecommerce.marketplace.event.OrderStatusUpdatedEvent;
import com.ecommerce.marketplace.exception.EmptyCartException;
import com.ecommerce.marketplace.exception.InsufficientStockException;
import com.ecommerce.marketplace.exception.ResourceNotFoundException;
import com.ecommerce.marketplace.repository.CartRepository;
import com.ecommerce.marketplace.repository.OrderRepository;
import com.ecommerce.marketplace.repository.ProductRepository;
import com.ecommerce.marketplace.repository.UserRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.Comparator;

@Service
public class OrderService {

    private final CartRepository cartRepository;
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final CouponService couponService;
    private final VendorLedgerService vendorLedgerService;

    public OrderService(
            CartRepository cartRepository,
            OrderRepository orderRepository,
            UserRepository userRepository,
            ProductRepository productRepository,
            ApplicationEventPublisher eventPublisher,
            CouponService couponService,
            VendorLedgerService vendorLedgerService) {
        this.cartRepository = cartRepository;
        this.orderRepository = orderRepository;
        this.userRepository = userRepository;
        this.productRepository = productRepository;
        this.eventPublisher = eventPublisher;
        this.couponService = couponService;
        this.vendorLedgerService = vendorLedgerService;
    }

    @Transactional
    public OrderResponse placeOrder(String username) {
        User customer = userRepository.findByUsername(username)
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Cart cart = cartRepository.findByUser(customer)
            .orElseThrow(() -> new EmptyCartException("Cart is empty"));

        if (cart.getItems() == null || cart.getItems().isEmpty()) {
            throw new EmptyCartException("Cart is empty");
        }

        BigDecimal discountAmount = cart.getCoupon() == null
            ? BigDecimal.ZERO
            : couponService.consumeCoupon(cart.getCoupon().getId(), cart.getItems());

        List<CheckoutItemRequest> checkoutItems = cart.getItems().stream()
                .map(item -> new CheckoutItemRequest(item.getProduct().getId(), item.getQuantity()))
                .toList();
        OrderResponse response = createOrder(customer, checkoutItems, discountAmount,
                cart.getCoupon() == null ? null : cart.getCoupon().getCode(), null);
        cart.getItems().clear();
        cart.setCoupon(null);
        cartRepository.save(cart);
        return response;
    }

    @Transactional
    public OrderResponse checkout(String username, CheckoutRequest request) {
        if (request == null || request.items() == null || request.items().isEmpty()) {
            throw new IllegalArgumentException("Checkout must contain at least one item");
        }

        User customer = userRepository.findByUsername(username)
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        Cart cart = cartRepository.findByUser(customer).orElse(null);
        BigDecimal discountAmount = BigDecimal.ZERO;
        String couponCode = null;
        if (request.couponCode() != null && !request.couponCode().isBlank()) {
            if (cart == null || cart.getCoupon() == null
                    || !cart.getCoupon().getCode().equalsIgnoreCase(request.couponCode().trim())
                    || !sameItemsAsCart(request.items(), cart)) {
                throw new IllegalArgumentException("The cart changed or the applied coupon is no longer valid");
            }
            discountAmount = couponService.consumeCoupon(cart.getCoupon().getId(), cart.getItems());
            couponCode = cart.getCoupon().getCode();
        }

        OrderResponse response = createOrder(customer, request.items(), discountAmount, couponCode, request.shippingAddress());
        if (cart != null) {
            cart.getItems().clear();
            cart.setCoupon(null);
            cartRepository.save(cart);
        }
        return response;
    }

    private boolean sameItemsAsCart(List<CheckoutItemRequest> requestedItems, Cart cart) {
        Map<Long, Integer> requestQuantities = new TreeMap<>();
        for (CheckoutItemRequest item : requestedItems) {
            if (item == null || item.productId() == null || item.quantity() == null) {
                return false;
            }
            requestQuantities.merge(item.productId(), item.quantity(), Integer::sum);
        }
        Map<Long, Integer> cartQuantities = new TreeMap<>();
        cart.getItems().forEach(item -> cartQuantities.put(item.getProduct().getId(), item.getQuantity()));
        return requestQuantities.equals(cartQuantities);
    }

    private OrderResponse createOrder(
            User customer,
            List<CheckoutItemRequest> requestedItems,
            BigDecimal discountAmount,
            String couponCode,
            com.ecommerce.marketplace.dto.ShippingAddress shippingAddress) {
        Map<Long, Integer> quantities = new TreeMap<>();
        for (CheckoutItemRequest item : requestedItems) {
            if (item == null || item.productId() == null || item.productId() <= 0
                    || item.quantity() == null || item.quantity() <= 0) {
                throw new IllegalArgumentException("Each checkout item needs a positive product ID and quantity");
            }
            try {
                quantities.merge(item.productId(), item.quantity(), Math::addExact);
            } catch (ArithmeticException exception) {
                throw new IllegalArgumentException("Combined product quantity is too large", exception);
            }
        }

        Order order = new Order();
        order.setUser(customer);
        order.setDiscountAmount(discountAmount);
        order.setCouponCode(couponCode);
        order.setShippingAddress(shippingAddress == null ? null : shippingAddress.toEntity());
        BigDecimal subtotal = BigDecimal.ZERO;
        Map<Long, SubOrder> vendorOrders = new LinkedHashMap<>();

        // Deterministic lock ordering reduces deadlock risk for carts with multiple products.
        for (Map.Entry<Long, Integer> entry : quantities.entrySet()) {
            Product product = productRepository.findByIdForUpdate(entry.getKey())
                    .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + entry.getKey()));
            int quantity = entry.getValue();
            if (product.getStock() < quantity) {
                throw new InsufficientStockException("Insufficient stock for product: " + product.getName());
            }
            product.setStock(product.getStock() - quantity);

            SubOrder subOrder = vendorOrders.computeIfAbsent(product.getVendor().getId(), vendorId -> {
                SubOrder created = new SubOrder();
                created.setOrder(order);
                created.setVendor(product.getVendor());
                order.getSubOrders().add(created);
                return created;
            });

            OrderItem orderItem = new OrderItem();
            orderItem.setOrder(order);
            orderItem.setSubOrder(subOrder);
            orderItem.setProduct(product);
            orderItem.setQuantity(quantity);
            orderItem.setUnitPrice(product.getPrice());
            order.getItems().add(orderItem);
            subOrder.getItems().add(orderItem);

            BigDecimal lineTotal = product.getPrice().multiply(BigDecimal.valueOf(quantity));
            subOrder.setTotalAmount(subOrder.getTotalAmount().add(lineTotal));
            subtotal = subtotal.add(lineTotal);
        }

        if (discountAmount.compareTo(subtotal) > 0) {
            throw new IllegalArgumentException("Discount cannot exceed the order subtotal");
        }
        order.setSubtotalAmount(subtotal);
        order.setTotalAmount(subtotal.subtract(discountAmount));
        vendorOrders.values().forEach(vendorLedgerService::attachPendingRecord);
        Order savedOrder = orderRepository.save(order);
        eventPublisher.publishEvent(new OrderPlacedEvent(
                customer.getEmail(), savedOrder.getId(), savedOrder.getTotalAmount()));

        return OrderResponse.from(savedOrder);
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> getCustomerOrders(String username) {
        User customer = userRepository.findByUsername(username)
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        return orderRepository.findByUserOrderByPlacedAtDesc(customer).stream()
                .map(OrderResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public OrderResponse getCustomerOrder(String username, Long orderId) {
        User customer = userRepository.findByUsername(username)
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Order order = orderRepository.findByIdAndUser(orderId, customer)
            .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        return OrderResponse.from(order);
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> getVendorOrders(String username) {
        User vendor = userRepository.findByUsername(username)
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        return orderRepository.findAll().stream()
                .filter(order -> order.getItems().stream()
                        .anyMatch(item -> item.getProduct().getVendor().getId().equals(vendor.getId())))
                .sorted(Comparator.comparing(Order::getPlacedAt).reversed())
                .map(order -> OrderResponse.forVendor(order, vendor))
                .toList();
    }

    @Transactional
    public OrderResponse updateVendorOrderStatus(String username, Long orderId, UpdateOrderStatusRequest request) {
        if (request == null || request.status() == null) {
            throw new IllegalArgumentException("Order status is required");
        }

        User vendor = userRepository.findByUsername(username)
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Order order = orderRepository.findByIdForUpdate(orderId)
            .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        SubOrder subOrder = order.getSubOrders().stream()
                .filter(candidate -> candidate.getVendor().getId().equals(vendor.getId()))
                .findFirst()
                .orElse(null);

        if (subOrder == null) {
            List<OrderItem> vendorItems = order.getItems().stream()
                    .filter(item -> item.getProduct().getVendor().getId().equals(vendor.getId()))
                    .toList();
            if (vendorItems.isEmpty()) {
                throw new AccessDeniedException("You can only update orders for your products");
            }
            subOrder = new SubOrder();
            subOrder.setOrder(order);
            subOrder.setVendor(vendor);
            subOrder.setTotalAmount(vendorItems.stream()
                    .map(item -> item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                    .reduce(BigDecimal.ZERO, BigDecimal::add));
            vendorLedgerService.attachPendingRecord(subOrder);
            subOrder.getItems().addAll(vendorItems);
            for (OrderItem vendorItem : vendorItems) {
                vendorItem.setSubOrder(subOrder);
            }
            order.getSubOrders().add(subOrder);
        }

        OrderStatus previousStatus = order.getStatus();
        OrderStatus currentStatus = subOrder.getStatus();
        OrderStatus requestedStatus = request.status();
        if (currentStatus == requestedStatus) return OrderResponse.forVendor(order, vendor);
        if (!isAllowedTransition(currentStatus, requestedStatus)) {
            throw new IllegalArgumentException("Order cannot move from " + currentStatus + " to " + requestedStatus);
        }
        if (requestedStatus == OrderStatus.CANCELLED) restoreStock(subOrder.getItems());
        subOrder.setStatus(request.status());
        order.setStatus(aggregateStatus(order.getSubOrders()));
        Order savedOrder = orderRepository.save(order);
        if (previousStatus != savedOrder.getStatus()) {
            eventPublisher.publishEvent(new OrderStatusUpdatedEvent(
                order.getUser().getEmail(), order.getId(), order.getStatus()));
        }
        return OrderResponse.forVendor(savedOrder, vendor);
    }

    @Transactional
    public OrderResponse cancelCustomerOrder(String username, Long orderId) {
        User customer = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        Order order = orderRepository.findByIdAndUserForUpdate(orderId, customer)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));
        if (!isAllowedTransition(order.getStatus(), OrderStatus.CANCELLED)) {
            throw new IllegalArgumentException("This order can no longer be cancelled");
        }
        boolean fulfillmentStarted = order.getSubOrders().stream()
                .anyMatch(subOrder -> subOrder.getStatus() == OrderStatus.SHIPPED
                        || subOrder.getStatus() == OrderStatus.DELIVERED);
        if (fulfillmentStarted) {
            throw new IllegalArgumentException("An order cannot be cancelled after any vendor has shipped an item");
        }
        for (SubOrder subOrder : order.getSubOrders()) {
            if (subOrder.getStatus() != OrderStatus.CANCELLED) {
                restoreStock(subOrder.getItems());
                subOrder.setStatus(OrderStatus.CANCELLED);
            }
        }
        order.setStatus(OrderStatus.CANCELLED);
        return OrderResponse.from(orderRepository.save(order));
    }

    private boolean isAllowedTransition(OrderStatus current, OrderStatus requested) {
        if (requested == OrderStatus.CANCELLED) {
            return current == OrderStatus.PLACED || current == OrderStatus.PROCESSING;
        }
        return (current == OrderStatus.PLACED && requested == OrderStatus.PROCESSING)
                || (current == OrderStatus.PROCESSING && requested == OrderStatus.SHIPPED)
                || (current == OrderStatus.SHIPPED && requested == OrderStatus.DELIVERED);
    }

    private void restoreStock(List<OrderItem> items) {
        Map<Long, Integer> quantities = new TreeMap<>();
        items.forEach(item -> quantities.merge(item.getProduct().getId(), item.getQuantity(), Math::addExact));
        quantities.forEach((productId, quantity) -> {
            Product product = productRepository.findByIdForUpdate(productId)
                    .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + productId));
            product.setStock(Math.addExact(product.getStock(), quantity));
        });
    }

    private OrderStatus aggregateStatus(List<SubOrder> subOrders) {
        if (subOrders.isEmpty()) {
            return OrderStatus.PLACED;
        }
        if (subOrders.stream().allMatch(subOrder -> subOrder.getStatus() == OrderStatus.DELIVERED)) {
            return OrderStatus.DELIVERED;
        }
        if (subOrders.stream().allMatch(subOrder -> subOrder.getStatus() == OrderStatus.CANCELLED)) {
            return OrderStatus.CANCELLED;
        }
        if (subOrders.stream().anyMatch(subOrder -> subOrder.getStatus() == OrderStatus.SHIPPED)) {
            return OrderStatus.SHIPPED;
        }
        if (subOrders.stream().anyMatch(subOrder -> subOrder.getStatus() == OrderStatus.PROCESSING)) {
            return OrderStatus.PROCESSING;
        }
        return OrderStatus.PLACED;
    }
}
