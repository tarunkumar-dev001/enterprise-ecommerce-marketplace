package com.ecommerce.marketplace.service;

import com.ecommerce.marketplace.dto.CartItemRequest;
import com.ecommerce.marketplace.dto.CartResponse;
import com.ecommerce.marketplace.dto.ApplyCouponRequest;
import com.ecommerce.marketplace.dto.UpdateCartItemRequest;
import com.ecommerce.marketplace.entity.Cart;
import com.ecommerce.marketplace.entity.CartItem;
import com.ecommerce.marketplace.entity.Coupon;
import com.ecommerce.marketplace.entity.Product;
import com.ecommerce.marketplace.entity.ProductModerationStatus;
import com.ecommerce.marketplace.exception.BusinessException;
import org.springframework.http.HttpStatus;
import com.ecommerce.marketplace.entity.User;
import com.ecommerce.marketplace.exception.InsufficientStockException;
import com.ecommerce.marketplace.exception.ResourceNotFoundException;
import com.ecommerce.marketplace.repository.CartItemRepository;
import com.ecommerce.marketplace.repository.CartRepository;
import com.ecommerce.marketplace.repository.ProductRepository;
import com.ecommerce.marketplace.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final CouponService couponService;

    public CartService(
            CartRepository cartRepository,
            CartItemRepository cartItemRepository,
            UserRepository userRepository,
            ProductRepository productRepository,
            CouponService couponService) {
        this.cartRepository = cartRepository;
        this.cartItemRepository = cartItemRepository;
        this.userRepository = userRepository;
        this.productRepository = productRepository;
        this.couponService = couponService;
    }

    @Transactional
    public CartResponse addItemToCart(String username, CartItemRequest request) {
        if (request == null || request.productId() == null || request.quantity() == null || request.quantity() <= 0) {
            throw new IllegalArgumentException("Valid product ID and quantity are required");
        }

        User customer = userRepository.findByUsername(username)
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Product product = productRepository.findById(request.productId())
            .orElseThrow(() -> new ResourceNotFoundException("Product not found"));

        ensureApproved(product);

        if (product.getStock() <= 0) {
            throw new InsufficientStockException("Product is out of stock: " + product.getName());
        }

        Cart cart = cartRepository.findByUser(customer)
                .orElseGet(() -> {
                    Cart newCart = new Cart();
                    newCart.setUser(customer);
                    return cartRepository.save(newCart);
                });

        CartItem item = cartItemRepository.findByCartAndProduct(cart, product)
                .orElseGet(() -> {
                    CartItem newItem = new CartItem();
                    newItem.setCart(cart);
                    newItem.setProduct(product);
                    newItem.setQuantity(0);
                    return newItem;
                });

        int newQuantity = item.getQuantity() + request.quantity();
        if (newQuantity > product.getStock()) {
            throw new InsufficientStockException("Requested quantity exceeds available stock for product: "
                    + product.getName());
        }

        item.setQuantity(newQuantity);
        if (item.getId() == null) {
            cart.getItems().add(item);
        }
        cartItemRepository.save(item);

        return cartResponse(cartRepository.save(cart));
    }

    @Transactional
    public CartResponse applyCoupon(String username, ApplyCouponRequest request) {
        if (request == null || request.code() == null || request.code().isBlank()) {
            throw new IllegalArgumentException("Coupon code is required");
        }
        User customer = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        Cart cart = cartRepository.findByUser(customer)
                .orElseThrow(() -> new IllegalArgumentException("Cart is empty"));
        if (cart.getItems() == null || cart.getItems().isEmpty()) {
            throw new IllegalArgumentException("Cart is empty");
        }

        Coupon coupon = couponService.findUsableCoupon(request.code());
        couponService.calculateDiscount(coupon, cart.getItems());
        cart.setCoupon(coupon);
        return cartResponse(cartRepository.save(cart));
    }

        @Transactional
        public CartResponse updateItemQuantity(String username, Long productId, UpdateCartItemRequest request) {
        if (request == null || request.quantity() == null || request.quantity() < 1) {
            throw new IllegalArgumentException("Quantity must be at least 1");
        }
        User customer = userRepository.findByUsername(username)
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        Cart cart = cartRepository.findByUser(customer)
            .orElseThrow(() -> new ResourceNotFoundException("Cart not found"));
        CartItem item = cartItemRepository.findByCartAndProduct_Id(cart, productId)
            .orElseThrow(() -> new ResourceNotFoundException("Cart item not found"));
        ensureApproved(item.getProduct());
        if (request.quantity() > item.getProduct().getStock()) {
            throw new InsufficientStockException("Requested quantity exceeds available stock for product: "
                + item.getProduct().getName());
        }

        item.setQuantity(request.quantity());
        cartItemRepository.save(item);
        clearCouponIfNotApplicable(cart);
        return cartResponse(cartRepository.save(cart));
        }

        @Transactional
        public CartResponse removeItem(String username, Long productId) {
        User customer = userRepository.findByUsername(username)
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        Cart cart = cartRepository.findByUser(customer)
            .orElseThrow(() -> new ResourceNotFoundException("Cart not found"));
        CartItem item = cartItemRepository.findByCartAndProduct_Id(cart, productId)
            .orElseThrow(() -> new ResourceNotFoundException("Cart item not found"));

        cart.getItems().remove(item);
        cartItemRepository.delete(item);
        clearCouponIfNotApplicable(cart);
        return cartResponse(cartRepository.save(cart));
        }

        @Transactional
        public CartResponse removeCoupon(String username) {
        User customer = userRepository.findByUsername(username)
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        Cart cart = cartRepository.findByUser(customer)
            .orElseThrow(() -> new ResourceNotFoundException("Cart not found"));
        cart.setCoupon(null);
        return CartResponse.from(cartRepository.save(cart));
        }

    @Transactional
    public CartResponse getCart(String username) {
        User customer = userRepository.findByUsername(username)
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Cart cart = cartRepository.findByUser(customer)
                .orElseGet(() -> {
                    Cart newCart = new Cart();
                    newCart.setUser(customer);
                    return cartRepository.save(newCart);
                });

        return cartResponse(cart);
    }

    private CartResponse cartResponse(Cart cart) {
        if (cart.getCoupon() == null) {
            return CartResponse.from(cart);
        }
        return CartResponse.from(cart, couponService.calculateDiscount(cart.getCoupon(), CartResponse.availableItems(cart)));
    }

    private void clearCouponIfNotApplicable(Cart cart) {
        Coupon coupon = cart.getCoupon();
        if (coupon != null && cart.getItems().stream().noneMatch(item -> coupon.getVendor() == null
                || coupon.getVendor().getId().equals(item.getProduct().getVendor().getId()))) {
            cart.setCoupon(null);
        }
    }

    private void ensureApproved(Product product) {
        if (product.getModerationStatus() != ProductModerationStatus.APPROVED) {
            throw new BusinessException("Product is not currently available for purchase.", HttpStatus.CONFLICT);
        }
    }
}
