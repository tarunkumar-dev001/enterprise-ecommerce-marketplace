package com.ecommerce.marketplace.repository;

import com.ecommerce.marketplace.entity.Cart;
import com.ecommerce.marketplace.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CartRepository extends JpaRepository<Cart, Long> {
    Optional<Cart> findByUser(User user);
}
