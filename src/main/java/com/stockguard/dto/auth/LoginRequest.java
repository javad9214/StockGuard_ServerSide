package com.stockguard.dto.auth;

import lombok.Data;

@Data
public class LoginRequest {
    private String phoneNumber;
    private String password;
}
