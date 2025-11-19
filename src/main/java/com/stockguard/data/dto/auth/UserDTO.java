package com.stockguard.data.dto.auth;

import com.stockguard.data.entity.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

// User DTO
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserDTO {

    private Long id;
    private String phoneNumber;
    private String fullName;
    private String profileImageUrl;
    private User.UserRole role;
    private Boolean enabled;
    private LocalDateTime createdAt;
    private LocalDateTime lastLogin;
}