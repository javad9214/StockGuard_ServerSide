package com.stockguard.service.impl;


import com.stockguard.dto.auth.LoginRequest;
import com.stockguard.dto.auth.LoginResponse;
import com.stockguard.dto.auth.RegisterRequest;
import com.stockguard.domain.User;
import com.stockguard.repository.UserRepository;
import com.stockguard.security.JwtUtil;
import com.stockguard.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    @Override
    public void register(RegisterRequest request) {
        // Check if user already exists
        if (userRepository.existsByPhoneNumber(request.getPhoneNumber())) {
            throw new RuntimeException("PhoneNumber already registered");
        }

        // Create new user
        User user = new User();
        user.setPhoneNumber(request.getPhoneNumber());
        user.setPassword(passwordEncoder.encode(request.getPassword())); // Hash password
        user.setFullName(request.getFullName());
        user.setEnabled(true);
        user.setRole("USER");
        user.setCreatedAt(LocalDateTime.now());

        userRepository.save(user);
    }

    @Override
    public LoginResponse login(LoginRequest request) {
        // Find user
        User user = userRepository.findByPhoneNumber(request.getPhoneNumber())
                .orElseThrow(() -> new RuntimeException("Invalid PhoneNumber or password"));

        // Check if user is enabled
        if (!user.isEnabled()) {
            throw new RuntimeException("Account is disabled");
        }

        // Verify password
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new RuntimeException("Invalid phoneNumber or password");
        }

        // Update last login
        user.setLastLogin(LocalDateTime.now());
        userRepository.save(user);

        // Generate JWT token
        String token = jwtUtil.generateToken(user.getPhoneNumber(), user.getRole());

        return new LoginResponse(token, user.getPhoneNumber(), user.getFullName(), user.getRole());
    }

    @Override
    public boolean validateToken(String token) {
        try {
            String phoneNumber = jwtUtil.extractPhoneNumber(token);
            User user = userRepository.findByPhoneNumber(phoneNumber)
                    .orElseThrow(() -> new RuntimeException("User not found"));
            return jwtUtil.validateToken(token, user.getPhoneNumber());
        } catch (Exception e) {
            return false;
        }
    }
}
