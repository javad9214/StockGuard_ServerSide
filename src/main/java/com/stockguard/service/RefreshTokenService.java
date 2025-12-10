package com.stockguard.service;

import com.stockguard.data.entity.RefreshToken;
import com.stockguard.data.entity.User;

public interface RefreshTokenService {

    RefreshToken createRefreshToken(User user, String deviceId);

    RefreshToken verifyExpiration(RefreshToken token);

    RefreshToken findByToken(String token);

    void revokeToken(String token);

    void revokeAllUserTokens(User user);

    void deleteExpiredTokens();
}