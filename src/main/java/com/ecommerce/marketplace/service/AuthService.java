package com.ecommerce.marketplace.service;

import com.ecommerce.marketplace.dto.LoginRequest;
import com.ecommerce.marketplace.dto.LoginResponse;
import com.ecommerce.marketplace.dto.SignupRequest;
import com.ecommerce.marketplace.entity.User;
import com.ecommerce.marketplace.repository.UserRepository;
import com.ecommerce.marketplace.security.JwtUtils;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Set;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtUtils jwtUtils;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            AuthenticationManager authenticationManager,
            JwtUtils jwtUtils) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtUtils = jwtUtils;
    }

    public String registerUser(SignupRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new IllegalArgumentException("Username is already taken");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email is already registered");
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        String role = "VENDOR".equals(request.getAccountType()) ? "ROLE_VENDOR" : "ROLE_CUSTOMER";
        user.setRoles(Set.of(role));
        userRepository.save(user);

        return "User registered successfully";
    }

    public LoginResponse authenticateUser(LoginRequest request) {
        var authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getUsername(), request.getPassword()));

        User user = userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new IllegalStateException("Authenticated user could not be found"));

        String token = jwtUtils.generateToken(user.getUsername());
        LoginResponse.UserInfo userInfo = new LoginResponse.UserInfo(
                user.getId(), user.getUsername(), user.getEmail(), user.getRoles());

        return new LoginResponse(token, userInfo);
    }
}
