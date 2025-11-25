package com.stockguard.data.dto.auth.request;

import com.stockguard.data.entity.User;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

// Admin Update User Request
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminUpdateUserRequest {

    private String fullName;
    private User.UserRole role;
    private Boolean enabled;
    private String profileImageUrl;
}