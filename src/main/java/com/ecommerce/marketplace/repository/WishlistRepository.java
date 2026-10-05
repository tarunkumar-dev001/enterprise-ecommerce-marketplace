package com.ecommerce.marketplace.repository;

import com.ecommerce.marketplace.entity.User;
import com.ecommerce.marketplace.entity.Wishlist;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface WishlistRepository extends JpaRepository<Wishlist, Long> {

    @EntityGraph(attributePaths = {"items", "items.product"})
    Optional<Wishlist> findByUser(User user);
}