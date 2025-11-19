package com.stockguard.controller;


import com.stockguard.data.dto.auth.UserDTO;
import com.stockguard.data.dto.auth.request.ChangePasswordRequestDTO;
import com.stockguard.data.dto.auth.request.LoginRequestDTO;
import com.stockguard.data.dto.auth.request.RegisterRequestDTO;
import com.stockguard.data.dto.auth.request.UpdateProfileRequestDTO;
import com.stockguard.data.dto.auth.response.AuthResponseDTO;
import com.stockguard.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /**
     * Register a new user
     * POST /api/auth/register
     */
    @PostMapping("/register")
    public ResponseEntity<AuthResponseDTO> register(@Valid @RequestBody RegisterRequestDTO requestDTO) {
        AuthResponseDTO response = userService.register(requestDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Login user
     * POST /api/auth/login
     */
    @PostMapping("/login")
    public ResponseEntity<AuthResponseDTO> login(@Valid @RequestBody LoginRequestDTO requestDTO) {
        AuthResponseDTO response = userService.login(requestDTO);
        return ResponseEntity.ok(response);
    }

    /**
     * Get current user profile
     * GET /api/auth/profile
     * Requires authentication
     */
    @GetMapping("/profile")
    public ResponseEntity<UserDTO> getProfile() {
        Long userId = getCurrentUserId();
        UserDTO profile = userService.getUserProfile(userId);
        return ResponseEntity.ok(profile);
    }

    /**
     * Update current user profile
     * PUT /api/auth/profile
     * Requires authentication
     */
    @PutMapping("/profile")
    public ResponseEntity<UserDTO> updateProfile(@Valid @RequestBody UpdateProfileRequestDTO requestDTO) {
        Long userId = getCurrentUserId();
        UserDTO profile = userService.updateProfile(userId, requestDTO);
        return ResponseEntity.ok(profile);
    }

    /**
     * Change password
     * POST /api/auth/change-password
     * Requires authentication
     */
    @PostMapping("/change-password")
    public ResponseEntity<String> changePassword(@Valid @RequestBody ChangePasswordRequestDTO requestDTO) {
        Long userId = getCurrentUserId();
        userService.changePassword(userId, requestDTO);
        return ResponseEntity.ok("Password changed successfully");
    }

    /**
     * Admin: Unlock user account
     * POST /api/auth/admin/unlock/{userId}
     * Requires ADMIN role
     */
    @PostMapping("/admin/unlock/{userId}")
    public ResponseEntity<String> unlockAccount(@PathVariable Long userId) {
        userService.unlockAccount(userId);
        return ResponseEntity.ok("Account unlocked successfully");
    }

    /**
     * Admin: Enable/Disable user account
     * POST /api/auth/admin/toggle-status/{userId}?enabled=true
     * Requires ADMIN role
     */
    @PostMapping("/admin/toggle-status/{userId}")
    public ResponseEntity<String> toggleUserStatus(
            @PathVariable Long userId,
            @RequestParam Boolean enabled) {
        userService.toggleUserStatus(userId, enabled);
        return ResponseEntity.ok("User status updated successfully");
    }

    /**
     * Helper method to get current authenticated user ID from JWT
     */
    private Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        // This assumes you have a custom UserDetails or the userId is stored in the principal
        // You'll need to adjust this based on your JWT filter implementation
        if (authentication != null && authentication.getPrincipal() instanceof Long) {
            return (Long) authentication.getPrincipal();
        }

        throw new IllegalStateException("User not authenticated");
    }
}