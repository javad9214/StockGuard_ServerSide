package com.stockguard.service.impl;

import com.stockguard.data.dto.auth.AdminUserDTO;
import com.stockguard.data.dto.auth.UserStatisticsDTO;
import com.stockguard.data.dto.auth.request.AdminCreateUserRequest;
import com.stockguard.data.dto.auth.request.AdminResetPasswordRequest;
import com.stockguard.data.dto.auth.request.AdminUpdateUserRequest;
import com.stockguard.data.dto.auth.response.AdminUserListResponse;
import com.stockguard.data.entity.User;

import com.stockguard.repository.UserRepository;
import com.stockguard.service.AdminService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminServiceImpl implements AdminService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional(readOnly = true)
    public AdminUserListResponse getAllUsers(
            Pageable pageable,
            String search,
            Boolean enabled,
            Boolean locked) {

        log.info("Fetching users with filters - search: {}, enabled: {}, locked: {}",
                search, enabled, locked);

        Page<User> usersPage = userRepository.findAllWithFilters(search, enabled, locked, pageable);

        List<AdminUserDTO> users = usersPage.getContent().stream()
                .map(this::mapToAdminUserDTO)
                .collect(Collectors.toList());

        return AdminUserListResponse.builder()
                .users(users)
                .currentPage(usersPage.getNumber())
                .totalPages(usersPage.getTotalPages())
                .totalElements(usersPage.getTotalElements())
                .pageSize(usersPage.getSize())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public AdminUserDTO getUserById(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found with ID: " + userId));
        return mapToAdminUserDTO(user);
    }

    @Override
    @Transactional
    public AdminUserDTO createUser(AdminCreateUserRequest request) {
        log.info("Admin creating new user with phone: {}", request.getPhoneNumber());

        // Check if phone number already exists
        if (userRepository.existsByPhoneNumber(request.getPhoneNumber())) {
            throw new IllegalArgumentException("Phone number already exists");
        }

        User user = new User();
        user.setPhoneNumber(request.getPhoneNumber());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setFullName(request.getFullName());
        user.setRole(request.getRole());
        user.setEnabled(request.getEnabled());
        user.setAccountLocked(false);
        user.setFailedLoginAttempts(0);

        User savedUser = userRepository.save(user);
        log.info("User created successfully with ID: {}", savedUser.getId());

        return mapToAdminUserDTO(savedUser);
    }

    @Override
    @Transactional
    public AdminUserDTO updateUser(Long userId, AdminUpdateUserRequest request) {
        log.info("Admin updating user: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found with ID: " + userId));

        if (request.getFullName() != null) {
            user.setFullName(request.getFullName());
        }
        if (request.getRole() != null) {
            user.setRole(request.getRole());
        }
        if (request.getEnabled() != null) {
            user.setEnabled(request.getEnabled());
        }
        if (request.getProfileImageUrl() != null) {
            user.setProfileImageUrl(request.getProfileImageUrl());
        }

        User updatedUser = userRepository.save(user);
        log.info("User updated successfully: {}", userId);

        return mapToAdminUserDTO(updatedUser);
    }

    @Override
    @Transactional
    public void deleteUser(Long userId) {
        log.info("Admin deleting user: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found with ID: " + userId));

        userRepository.delete(user);
        log.info("User deleted successfully: {}", userId);
    }

    @Override
    @Transactional
    public AdminUserDTO toggleUserStatus(Long userId) {
        log.info("Admin toggling user status: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found with ID: " + userId));

        user.setEnabled(!user.getEnabled());
        User updatedUser = userRepository.save(user);

        log.info("User status toggled to {}: {}", updatedUser.getEnabled(), userId);
        return mapToAdminUserDTO(updatedUser);
    }

    @Override
    @Transactional
    public AdminUserDTO unlockAccount(Long userId) {
        log.info("Admin unlocking account: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found with ID: " + userId));

        user.setAccountLocked(false);
        user.setFailedLoginAttempts(0);
        user.setAccountLockedAt(null);

        User updatedUser = userRepository.save(user);
        log.info("Account unlocked successfully: {}", userId);

        return mapToAdminUserDTO(updatedUser);
    }

    @Override
    @Transactional
    public void resetPassword(Long userId, AdminResetPasswordRequest request) {
        log.info("Admin resetting password for user: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found with ID: " + userId));

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        log.info("Password reset successfully for user: {}", userId);
    }

    @Override
    @Transactional
    public AdminUserDTO changeUserRole(Long userId, String role) {
        log.info("Admin changing user role: {} to {}", userId, role);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found with ID: " + userId));

        try {
            User.UserRole userRole = User.UserRole.valueOf(role.toUpperCase());
            user.setRole(userRole);
            User updatedUser = userRepository.save(user);

            log.info("User role changed successfully: {}", userId);
            return mapToAdminUserDTO(updatedUser);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid role: " + role);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public UserStatisticsDTO getUserStatistics() {
        log.info("Fetching user statistics");

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startOfDay = now.toLocalDate().atStartOfDay();
        LocalDateTime startOfWeek = now.minusWeeks(1);
        LocalDateTime startOfMonth = now.minusMonths(1);

        return UserStatisticsDTO.builder()
                .totalUsers(userRepository.count())
                .activeUsers(userRepository.countActiveUsers())
                .lockedUsers(userRepository.countByAccountLocked(true))
                .disabledUsers(userRepository.countByEnabled(false))
                .newUsersToday(userRepository.countByCreatedAtAfter(startOfDay))
                .newUsersThisWeek(userRepository.countByCreatedAtAfter(startOfWeek))
                .newUsersThisMonth(userRepository.countByCreatedAtAfter(startOfMonth))
                .build();
    }

    private AdminUserDTO mapToAdminUserDTO(User user) {
        return AdminUserDTO.builder()
                .id(user.getId())
                .phoneNumber(user.getPhoneNumber())
                .fullName(user.getFullName())
                .profileImageUrl(user.getProfileImageUrl())
                .role(user.getRole())
                .enabled(user.getEnabled())
                .accountLocked(user.getAccountLocked())
                .failedLoginAttempts(user.getFailedLoginAttempts())
                .deviceToken(user.getDeviceToken())
                .deviceId(user.getDeviceId())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .lastLogin(user.getLastLogin())
                .accountLockedAt(user.getAccountLockedAt())
                .build();
    }
}
