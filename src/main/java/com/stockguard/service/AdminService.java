package com.stockguard.service;


import com.stockguard.data.dto.auth.AdminUserDTO;

import com.stockguard.data.dto.auth.UserStatisticsDTO;
import com.stockguard.data.dto.auth.request.AdminCreateUserRequest;

import com.stockguard.data.dto.auth.request.AdminResetPasswordRequest;
import com.stockguard.data.dto.auth.request.AdminUpdateUserRequest;
import com.stockguard.data.dto.auth.response.AdminUserListResponse;
import org.springframework.data.domain.Pageable;

public interface AdminService {

    /**
     * Get all users with pagination and filters
     */
    AdminUserListResponse getAllUsers(
            Pageable pageable,
            String search,
            Boolean enabled,
            Boolean locked
    );

    /**
     * Get user details by ID
     */
    AdminUserDTO getUserById(Long userId);

    /**
     * Create new user (admin function)
     */
    AdminUserDTO createUser(AdminCreateUserRequest request);

    /**
     * Update user details
     */
    AdminUserDTO updateUser(Long userId, AdminUpdateUserRequest request);

    /**
     * Delete user
     */
    void deleteUser(Long userId);

    /**
     * Toggle user enabled status
     */
    AdminUserDTO toggleUserStatus(Long userId);

    /**
     * Unlock user account
     */
    AdminUserDTO unlockAccount(Long userId);

    /**
     * Reset user password
     */
    void resetPassword(Long userId, AdminResetPasswordRequest request);

    /**
     * Change user role
     */
    AdminUserDTO changeUserRole(Long userId, String role);

    /**
     * Get user statistics
     */
    UserStatisticsDTO getUserStatistics();
}
