package com.ecommerce.marketplace.repository;

import com.ecommerce.marketplace.entity.Wishlist;
import com.ecommerce.marketplace.entity.WishlistItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface WishlistItemRepository extends JpaRepository<WishlistItem, Long> {

    boolean existsByWishlistAndProduct_Id(Wishlist wishlist, Long productId);

    Optional<WishlistItem> findByWishlistAndProduct_Id(Wishlist wishlist, Long productId);
}