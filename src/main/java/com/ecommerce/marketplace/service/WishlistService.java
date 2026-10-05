package com.ecommerce.marketplace.service;

import com.ecommerce.marketplace.dto.WishlistItemRequest;
import com.ecommerce.marketplace.dto.WishlistResponse;
import com.ecommerce.marketplace.dto.CartItemRequest;
import com.ecommerce.marketplace.dto.CartResponse;
import com.ecommerce.marketplace.entity.Product;
import com.ecommerce.marketplace.entity.User;
import com.ecommerce.marketplace.entity.Wishlist;
import com.ecommerce.marketplace.entity.WishlistItem;
import com.ecommerce.marketplace.exception.ResourceNotFoundException;
import com.ecommerce.marketplace.repository.ProductRepository;
import com.ecommerce.marketplace.repository.UserRepository;
import com.ecommerce.marketplace.repository.WishlistItemRepository;
import com.ecommerce.marketplace.repository.WishlistRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WishlistService {

    private final WishlistRepository wishlistRepository;
    private final WishlistItemRepository wishlistItemRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final CartService cartService;

    public WishlistService(
            WishlistRepository wishlistRepository,
            WishlistItemRepository wishlistItemRepository,
            ProductRepository productRepository,
            UserRepository userRepository,
            CartService cartService) {
        this.wishlistRepository = wishlistRepository;
        this.wishlistItemRepository = wishlistItemRepository;
        this.productRepository = productRepository;
        this.userRepository = userRepository;
        this.cartService = cartService;
    }

    @Transactional
    public WishlistResponse getWishlist(String username) {
        User customer = findUser(username);
        Wishlist wishlist = wishlistRepository.findByUser(customer)
                .orElseGet(() -> createWishlist(customer));
        return WishlistResponse.from(wishlist);
    }

    @Transactional
    public WishlistResponse addItem(String username, WishlistItemRequest request) {
        if (request == null || request.productId() == null) {
            throw new IllegalArgumentException("Product ID is required");
        }
        User customer = findUser(username);
        Product product = productRepository.findById(request.productId())
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));
        Wishlist wishlist = wishlistRepository.findByUser(customer)
                .orElseGet(() -> createWishlist(customer));

        if (!wishlistItemRepository.existsByWishlistAndProduct_Id(wishlist, product.getId())) {
            WishlistItem item = new WishlistItem();
            item.setWishlist(wishlist);
            item.setProduct(product);
            wishlist.getItems().add(wishlistItemRepository.save(item));
        }
        return WishlistResponse.from(wishlist);
    }

    @Transactional
    public WishlistResponse removeItem(String username, Long productId) {
        User customer = findUser(username);
        Wishlist wishlist = wishlistRepository.findByUser(customer)
                .orElseThrow(() -> new ResourceNotFoundException("Wishlist not found"));
        WishlistItem item = wishlistItemRepository.findByWishlistAndProduct_Id(wishlist, productId)
                .orElseThrow(() -> new ResourceNotFoundException("Wishlist item not found"));

        wishlist.getItems().remove(item);
        wishlistItemRepository.delete(item);
        return WishlistResponse.from(wishlist);
    }

    @Transactional
    public CartResponse moveItemToCart(String username, Long productId) {
        User customer = findUser(username);
        Wishlist wishlist = wishlistRepository.findByUser(customer)
                .orElseThrow(() -> new ResourceNotFoundException("Wishlist not found"));
        WishlistItem item = wishlistItemRepository.findByWishlistAndProduct_Id(wishlist, productId)
                .orElseThrow(() -> new ResourceNotFoundException("Wishlist item not found"));

        CartResponse cart = cartService.addItemToCart(username, new CartItemRequest(productId, 1));
        wishlist.getItems().remove(item);
        wishlistItemRepository.delete(item);
        return cart;
    }

    private User findUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    private Wishlist createWishlist(User customer) {
        Wishlist wishlist = new Wishlist();
        wishlist.setUser(customer);
        return wishlistRepository.save(wishlist);
    }
}
