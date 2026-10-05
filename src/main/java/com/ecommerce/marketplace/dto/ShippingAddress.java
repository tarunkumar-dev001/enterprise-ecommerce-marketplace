package com.ecommerce.marketplace.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ShippingAddress(
        @NotBlank @Size(max = 120) String fullName,
        @NotBlank @Pattern(regexp = "[+0-9()\\- ]{7,20}") String phone,
        @NotBlank @Size(max = 180) String addressLine,
        @NotBlank @Size(max = 80) String city,
        @NotBlank @Size(max = 80) String state,
        @NotBlank @Size(max = 20) String postalCode,
        @NotBlank @Size(max = 80) String country) {

    public com.ecommerce.marketplace.entity.OrderShippingAddress toEntity() {
        return new com.ecommerce.marketplace.entity.OrderShippingAddress(
                fullName, phone, addressLine, city, state, postalCode, country);
    }

    public static ShippingAddress from(com.ecommerce.marketplace.entity.OrderShippingAddress address) {
        if (address == null) return null;
        return new ShippingAddress(address.fullName(), address.phone(), address.addressLine(), address.city(),
                address.state(), address.postalCode(), address.country());
    }
}
