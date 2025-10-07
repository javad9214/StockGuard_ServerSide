package com.stockguard.service;

import com.stockguard.dto.auth.LoginRequest;
import com.stockguard.dto.auth.LoginResponse;
import com.stockguard.dto.auth.RegisterRequest;

public interface AuthService {

    void register(RegisterRequest user);

    LoginResponse login(LoginRequest request);

    boolean validateToken(String token);
}