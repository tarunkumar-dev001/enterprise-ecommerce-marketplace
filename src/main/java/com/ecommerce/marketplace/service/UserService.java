package com.ecommerce.marketplace.service;

import com.ecommerce.marketplace.dto.UpdateProfileRequest;
import com.ecommerce.marketplace.dto.UserProfileResponse;
import com.ecommerce.marketplace.entity.User;
import com.ecommerce.marketplace.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public UserProfileResponse getCurrentUserProfile(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        return UserProfileResponse.from(user);
    }

    public UserProfileResponse updateCurrentUserProfile(String username, UpdateProfileRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Profile update request is required");
        }

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        if (request.username() != null && !request.username().isBlank()) {
            String normalizedUsername = request.username().trim();
            if (!normalizedUsername.equals(user.getUsername()) && userRepository.existsByUsername(normalizedUsername)) {
                throw new IllegalArgumentException("Username is already taken");
            }
            user.setUsername(normalizedUsername);
        }

        if (request.email() != null && !request.email().isBlank()) {
            String normalizedEmail = request.email().trim();
            if (!normalizedEmail.equalsIgnoreCase(user.getEmail()) && userRepository.existsByEmail(normalizedEmail)) {
                throw new IllegalArgumentException("Email is already registered");
            }
            user.setEmail(normalizedEmail);
        }

        if (request.password() != null && !request.password().isBlank()) {
            user.setPassword(passwordEncoder.encode(request.password()));
        }

        return UserProfileResponse.from(userRepository.save(user));
    }
}
