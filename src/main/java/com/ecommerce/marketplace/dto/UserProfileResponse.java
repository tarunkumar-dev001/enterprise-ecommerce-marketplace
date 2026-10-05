package com.ecommerce.marketplace.dto;

import com.ecommerce.marketplace.entity.User;

import java.util.Set;

public record UserProfileResponse(Long id, String username, String email, Set<String> roles) {
    public UserProfileResponse {
        roles = roles == null ? Set.of() : Set.copyOf(roles);
    }

    public static UserProfileResponse from(User user) {
        return new UserProfileResponse(user.getId(), user.getUsername(), user.getEmail(), user.getRoles());
    }
}
