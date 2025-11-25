package com.stockguard.data.dto.auth;

import com.stockguard.data.entity.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

// Admin User List DTO with all details
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminUserDTO {

    private Long id;
    private String phoneNumber;
    private String fullName;
    private String profileImageUrl;
    private User.UserRole role;
    private Boolean enabled;
    private Boolean accountLocked;
    private Integer failedLoginAttempts;
    private String deviceToken;
    private String deviceId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime lastLogin;
    private LocalDateTime accountLockedAt;
}




