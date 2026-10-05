package com.ecommerce.marketplace.entity;

import jakarta.persistence.Embeddable;

@Embeddable
public record OrderShippingAddress(
        String fullName,
        String phone,
        String addressLine,
        String city,
        String state,
        String postalCode,
        String country) {
}
