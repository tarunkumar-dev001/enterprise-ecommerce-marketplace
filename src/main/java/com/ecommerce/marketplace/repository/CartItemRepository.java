package com.ecommerce.marketplace.repository;

import com.ecommerce.marketplace.entity.Cart;
import com.ecommerce.marketplace.entity.CartItem;
import com.ecommerce.marketplace.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CartItemRepository extends JpaRepository<CartItem, Long> {
    Optional<CartItem> findByCartAndProduct(Cart cart, Product product);
    Optional<CartItem> findByCartAndProduct_Id(Cart cart, Long productId);
    List<CartItem> findByCart(Cart cart);
}
