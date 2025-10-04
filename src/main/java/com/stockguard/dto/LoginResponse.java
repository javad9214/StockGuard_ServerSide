package com.stockguard.dto;

import lombok.Data;

@Data
public class LoginResponse {
    private String token;
    private String email;
    private String fullName;
    private String role;

    public LoginResponse(String token, String email, String fullName, String role) {
        this.token = token;
        this.email = email;
        this.fullName = fullName;
        this.role = role;
    }
}
