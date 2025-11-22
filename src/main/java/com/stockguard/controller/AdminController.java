package com.stockguard.controller;

import com.stockguard.data.dto.auth.AdminUserDTO;
import com.stockguard.data.dto.auth.UserStatisticsDTO;
import com.stockguard.data.dto.auth.request.AdminCreateUserRequest;
import com.stockguard.data.dto.auth.request.AdminResetPasswordRequest;
import com.stockguard.data.dto.auth.request.AdminUpdateUserRequest;
import com.stockguard.data.dto.auth.response.AdminUserListResponse;
import com.stockguard.service.AdminService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final AdminService adminService;

    /**
     * Get all users with pagination and filters
     * GET /api/admin/users?page=0&size=20&search=john&enabled=true&locked=false&sortBy=createdAt&sortDir=desc
     */
    @GetMapping
    public ResponseEntity<AdminUserListResponse> getAllUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Boolean enabled,
            @RequestParam(required = false) Boolean locked,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {

        Sort sort = sortDir.equalsIgnoreCase("asc")
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();

        Pageable pageable = PageRequest.of(page, size, sort);

        AdminUserListResponse response = adminService.getAllUsers(pageable, search, enabled, locked);
        return ResponseEntity.ok(response);
    }

    /**
     * Get user by ID
     * GET /api/admin/users/{userId}
     */
    @GetMapping("/{userId}")
    public ResponseEntity<AdminUserDTO> getUserById(@PathVariable Long userId) {
        AdminUserDTO user = adminService.getUserById(userId);
        return ResponseEntity.ok(user);
    }

    /**
     * Create new user
     * POST /api/admin/users
     */
    @PostMapping
    public ResponseEntity<AdminUserDTO> createUser(@Valid @RequestBody AdminCreateUserRequest request) {
        AdminUserDTO user = adminService.createUser(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(user);
    }

    /**
     * Update user
     * PUT /api/admin/users/{userId}
     */
    @PutMapping("/{userId}")
    public ResponseEntity<AdminUserDTO> updateUser(
            @PathVariable Long userId,
            @Valid @RequestBody AdminUpdateUserRequest request) {
        AdminUserDTO user = adminService.updateUser(userId, request);
        return ResponseEntity.ok(user);
    }

    /**
     * Delete user
     * DELETE /api/admin/users/{userId}
     */
    @DeleteMapping("/{userId}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long userId) {
        adminService.deleteUser(userId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Toggle user enabled/disabled status
     * POST /api/admin/users/{userId}/toggle-status
     */
    @PostMapping("/{userId}/toggle-status")
    public ResponseEntity<AdminUserDTO> toggleUserStatus(@PathVariable Long userId) {
        AdminUserDTO user = adminService.toggleUserStatus(userId);
        return ResponseEntity.ok(user);
    }

    /**
     * Unlock user account
     * POST /api/admin/users/{userId}/unlock
     */
    @PostMapping("/{userId}/unlock")
    public ResponseEntity<AdminUserDTO> unlockAccount(@PathVariable Long userId) {
        AdminUserDTO user = adminService.unlockAccount(userId);
        return ResponseEntity.ok(user);
    }

    /**
     * Reset user password
     * POST /api/admin/users/{userId}/reset-password
     */
    @PostMapping("/{userId}/reset-password")
    public ResponseEntity<String> resetPassword(
            @PathVariable Long userId,
            @Valid @RequestBody AdminResetPasswordRequest request) {
        adminService.resetPassword(userId, request);
        return ResponseEntity.ok("Password reset successfully");
    }

    /**
     * Change user role
     * POST /api/admin/users/{userId}/change-role?role=ADMIN
     */
    @PostMapping("/{userId}/change-role")
    public ResponseEntity<AdminUserDTO> changeUserRole(
            @PathVariable Long userId,
            @RequestParam String role) {
        AdminUserDTO user = adminService.changeUserRole(userId, role);
        return ResponseEntity.ok(user);
    }

    /**
     * Get user statistics
     * GET /api/admin/users/statistics
     */
    @GetMapping("/statistics")
    public ResponseEntity<UserStatisticsDTO> getUserStatistics() {
        UserStatisticsDTO statistics = adminService.getUserStatistics();
        return ResponseEntity.ok(statistics);
    }
}
