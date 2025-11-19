package com.stockguard.service.impl;



import com.stockguard.data.dto.auth.UserDTO;
import com.stockguard.data.dto.auth.request.ChangePasswordRequestDTO;
import com.stockguard.data.dto.auth.request.LoginRequestDTO;
import com.stockguard.data.dto.auth.request.RegisterRequestDTO;
import com.stockguard.data.dto.auth.request.UpdateProfileRequestDTO;
import com.stockguard.data.dto.auth.response.AuthResponseDTO;
import com.stockguard.data.entity.User;

import com.stockguard.repository.UserRepository;
import com.stockguard.security.JwtUtil;
import com.stockguard.service.UserService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    private static final int MAX_FAILED_ATTEMPTS = 5;

    @Override
    @Transactional
    public AuthResponseDTO register(RegisterRequestDTO requestDTO) {
        log.info("Registering new user with phone: {}", requestDTO.getPhoneNumber());

        // Check if user already exists
        if (userRepository.existsByPhoneNumber(requestDTO.getPhoneNumber())) {
            throw new IllegalArgumentException("Phone number already registered");
        }

        // Create new user
        User user = new User();
        user.setPhoneNumber(requestDTO.getPhoneNumber());
        user.setPassword(passwordEncoder.encode(requestDTO.getPassword()));
        user.setFullName(requestDTO.getFullName());
        user.setDeviceToken(requestDTO.getDeviceToken());
        user.setDeviceId(requestDTO.getDeviceId());
        user.setEnabled(true);
        user.setAccountLocked(false);
        user.setFailedLoginAttempts(0);
        user.setRole(User.UserRole.USER);
        user.setLastLogin(LocalDateTime.now());

        User savedUser = userRepository.save(user);
        log.info("User registered successfully with ID: {}", savedUser.getId());

        // Generate JWT token
        String token = jwtUtil.generateToken(
                savedUser.getPhoneNumber(),
                savedUser.getId(),
                savedUser.getRole().name()
        );

        return AuthResponseDTO.builder()
                .token(token)
                .tokenType("Bearer")
                .user(mapToUserDTO(savedUser))
                .message("Registration successful")
                .build();
    }

    @Override
    @Transactional
    public AuthResponseDTO login(LoginRequestDTO requestDTO) {
        log.info("Login attempt for phone: {}", requestDTO.getPhoneNumber());

        // Find user
        User user = userRepository.findByPhoneNumber(requestDTO.getPhoneNumber())
                .orElseThrow(() -> new IllegalArgumentException("Invalid phone number or password"));

        // Check if account is locked
        if (user.getAccountLocked()) {
            throw new IllegalStateException("Account is locked due to multiple failed login attempts. Please contact support.");
        }

        // Check if account is enabled
        if (!user.getEnabled()) {
            throw new IllegalStateException("Account is disabled. Please contact support.");
        }

        // Verify password
        if (!passwordEncoder.matches(requestDTO.getPassword(), user.getPassword())) {
            handleFailedLogin(user);
            throw new IllegalArgumentException("Invalid phone number or password");
        }

        // Reset failed attempts on successful login
        resetFailedAttempts(user);

        // Update last login and device info
        user.setLastLogin(LocalDateTime.now());
        if (requestDTO.getDeviceToken() != null) {
            user.setDeviceToken(requestDTO.getDeviceToken());
        }
        if (requestDTO.getDeviceId() != null) {
            user.setDeviceId(requestDTO.getDeviceId());
        }
        userRepository.save(user);

        log.info("User logged in successfully: {}", user.getId());

        // Generate JWT token
        String token = jwtUtil.generateToken(
                user.getPhoneNumber(),
                user.getId(),
                user.getRole().name()
        );

        return AuthResponseDTO.builder()
                .token(token)
                .tokenType("Bearer")
                .user(mapToUserDTO(user))
                .message("Login successful")
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public UserDTO getUserProfile(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        return mapToUserDTO(user);
    }

    @Override
    @Transactional
    public UserDTO updateProfile(Long userId, UpdateProfileRequestDTO requestDTO) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        if (requestDTO.getFullName() != null) {
            user.setFullName(requestDTO.getFullName());
        }
        if (requestDTO.getProfileImageUrl() != null) {
            user.setProfileImageUrl(requestDTO.getProfileImageUrl());
        }
        if (requestDTO.getDeviceToken() != null) {
            user.setDeviceToken(requestDTO.getDeviceToken());
        }

        User updatedUser = userRepository.save(user);
        log.info("User profile updated: {}", userId);

        return mapToUserDTO(updatedUser);
    }

    @Override
    @Transactional
    public void changePassword(Long userId, ChangePasswordRequestDTO requestDTO) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        // Verify old password
        if (!passwordEncoder.matches(requestDTO.getOldPassword(), user.getPassword())) {
            throw new IllegalArgumentException("Current password is incorrect");
        }

        // Update password
        user.setPassword(passwordEncoder.encode(requestDTO.getNewPassword()));
        userRepository.save(user);

        log.info("Password changed successfully for user: {}", userId);
    }

    @Override
    @Transactional
    public void unlockAccount(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        user.setAccountLocked(false);
        user.setFailedLoginAttempts(0);
        user.setAccountLockedAt(null);
        userRepository.save(user);

        log.info("Account unlocked for user: {}", userId);
    }

    @Override
    @Transactional
    public void toggleUserStatus(Long userId, Boolean enabled) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        user.setEnabled(enabled);
        userRepository.save(user);

        log.info("User {} status changed to: {}", userId, enabled ? "enabled" : "disabled");
    }

    // Helper methods

    private void handleFailedLogin(User user) {
        int attempts = user.getFailedLoginAttempts() + 1;
        user.setFailedLoginAttempts(attempts);

        if (attempts >= MAX_FAILED_ATTEMPTS) {
            user.setAccountLocked(true);
            user.setAccountLockedAt(LocalDateTime.now());
            log.warn("Account locked for user: {} due to {} failed attempts", user.getPhoneNumber(), attempts);
        }

        userRepository.save(user);
    }

    private void resetFailedAttempts(User user) {
        if (user.getFailedLoginAttempts() > 0) {
            user.setFailedLoginAttempts(0);
            userRepository.save(user);
        }
    }

    private UserDTO mapToUserDTO(User user) {
        return UserDTO.builder()
                .id(user.getId())
                .phoneNumber(user.getPhoneNumber())
                .fullName(user.getFullName())
                .profileImageUrl(user.getProfileImageUrl())
                .role(user.getRole())
                .enabled(user.getEnabled())
                .createdAt(user.getCreatedAt())
                .lastLogin(user.getLastLogin())
                .build();
    }
}