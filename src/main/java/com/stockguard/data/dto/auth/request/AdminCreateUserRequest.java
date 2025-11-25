package com.stockguard.data.dto.auth.request;

import com.stockguard.data.entity.User;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

// Admin Create User Request
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminCreateUserRequest {

    @NotNull(message = "Phone number is required")
    private String phoneNumber;

    @NotNull(message = "Password is required")
    private String password;

    @NotNull(message = "Full name is required")
    private String fullName;

    private User.UserRole role = User.UserRole.USER;

    private Boolean enabled = true;
}



