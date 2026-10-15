package com.ecommerce.marketplace.dto;

import com.ecommerce.marketplace.entity.Wishlist;
import com.ecommerce.marketplace.entity.ProductModerationStatus;

import java.util.List;

public record WishlistResponse(Long id, List<WishlistItemResponse> items) {

    public static WishlistResponse from(Wishlist wishlist) {
        List<WishlistItemResponse> items = wishlist.getItems().stream()
                .filter(item -> item.getProduct().getModerationStatus() == ProductModerationStatus.APPROVED)
                .map(WishlistItemResponse::from)
                .toList();
        return new WishlistResponse(wishlist.getId(), items);
    }
}
