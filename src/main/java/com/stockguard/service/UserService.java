package com.stockguard.service;


import com.stockguard.data.dto.auth.UserDTO;
import com.stockguard.data.dto.auth.request.ChangePasswordRequestDTO;
import com.stockguard.data.dto.auth.request.LoginRequestDTO;
import com.stockguard.data.dto.auth.request.RegisterRequestDTO;
import com.stockguard.data.dto.auth.request.UpdateProfileRequestDTO;
import com.stockguard.data.dto.auth.response.AuthResponseDTO;


public interface UserService {

    /**
     * Register a new user
     */
    AuthResponseDTO register(RegisterRequestDTO requestDTO);

    /**
     * Login user
     */
    AuthResponseDTO login(LoginRequestDTO requestDTO);

    /**
     * Get user profile by ID
     */
    UserDTO getUserProfile(Long userId);

    /**
     * Update user profile
     */
    UserDTO updateProfile(Long userId, UpdateProfileRequestDTO requestDTO);

    /**
     * Change user password
     */
    void changePassword(Long userId, ChangePasswordRequestDTO requestDTO);

    /**
     * Unlock user account (admin function)
     */
    void unlockAccount(Long userId);

    /**
     * Disable/Enable user account (admin function)
     */
    void toggleUserStatus(Long userId, Boolean enabled);
}