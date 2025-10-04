package com.stockguard.service;

import com.stockguard.dto.LoginRequest;
import com.stockguard.dto.LoginResponse;
import com.stockguard.dto.RegisterRequest;

public interface AuthService {

    void register(RegisterRequest user);

    LoginResponse login(LoginRequest request);

    boolean validateToken(String token);
}