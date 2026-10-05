package com.ecommerce.marketplace.dto;

import java.util.Set;

public record LoginResponse(String token, UserInfo user) {

    public record UserInfo(Long id, String username, String email, Set<String> roles) {
        public UserInfo {
            roles = Set.copyOf(roles);
        }
    }
}
