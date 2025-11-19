package com.stockguard.data.dto.auth.response;

import lombok.Data;

@Data
public class LoginResponse {
    private String token;
    private String phoneNumber;
    private String fullName;
    private String role;

    public LoginResponse(String token, String phoneNumber, String fullName, String role) {
        this.token = token;
        this.phoneNumber = phoneNumber;
        this.fullName = fullName;
        this.role = role;
    }
}
