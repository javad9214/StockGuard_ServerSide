package com.stockguard.dto.auth;

import lombok.Data;

@Data
public class RegisterRequest {
    private String phoneNumber;
    private String password;
    private String fullName;
}
