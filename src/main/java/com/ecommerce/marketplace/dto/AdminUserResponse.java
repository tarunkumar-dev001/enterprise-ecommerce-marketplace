package com.ecommerce.marketplace.dto;

import com.ecommerce.marketplace.entity.User;
import java.util.Set;

public record AdminUserResponse(Long id, String username, String email, Set<String> roles) {
    public static AdminUserResponse from(User user) {
        return new AdminUserResponse(user.getId(), user.getUsername(), user.getEmail(), Set.copyOf(user.getRoles()));
    }
}
