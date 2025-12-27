package com.stockguard.controller;

import com.stockguard.data.dto.auth.UserDTO;
import com.stockguard.data.dto.auth.request.ChangePasswordRequestDTO;
import com.stockguard.data.dto.auth.request.LoginRequestDTO;
import com.stockguard.data.dto.auth.request.RefreshTokenRequestDTO;
import com.stockguard.data.dto.auth.request.RegisterRequestDTO;
import com.stockguard.data.dto.auth.request.UpdateProfileRequestDTO;
import com.stockguard.data.dto.auth.response.AuthResponseDTO;
import com.stockguard.data.entity.RefreshToken;
import com.stockguard.data.entity.User;
import com.stockguard.repository.UserRepository;
import com.stockguard.security.JwtUtil;
import com.stockguard.service.RefreshTokenService;
import com.stockguard.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final RefreshTokenService refreshTokenService;
    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;

    @PostMapping("/register")
    public ResponseEntity<AuthResponseDTO> register(@Valid @RequestBody RegisterRequestDTO requestDTO) {
        AuthResponseDTO response = userService.register(requestDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponseDTO> login(@Valid @RequestBody LoginRequestDTO requestDTO) {
        AuthResponseDTO response = userService.login(requestDTO);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponseDTO> refreshToken(@Valid @RequestBody RefreshTokenRequestDTO request) {
        String requestRefreshToken = request.getRefreshToken();

        RefreshToken refreshToken = refreshTokenService.findByToken(requestRefreshToken);
        refreshToken = refreshTokenService.verifyExpiration(refreshToken);

        User user = refreshToken.getUser();

        String newAccessToken = jwtUtil.generateToken(
                user.getPhoneNumber(),
                user.getId(),
                user.getRole().name()
        );

        RefreshToken newRefreshToken = refreshTokenService.createRefreshToken(user, refreshToken.getDeviceId());

        refreshTokenService.revokeToken(requestRefreshToken);

        return ResponseEntity.ok(AuthResponseDTO.builder()
                .token(newAccessToken)
                .refreshToken(newRefreshToken.getToken())
                .tokenType("Bearer")
                .user(mapToUserDTO(user))
                .message("Token refreshed successfully")
                .build());
    }

    @GetMapping("/profile")
    public ResponseEntity<UserDTO> getProfile() {
        Long userId = getCurrentUserId();
        UserDTO profile = userService.getUserProfile(userId);
        return ResponseEntity.ok(profile);
    }

    @PutMapping("/profile")
    public ResponseEntity<UserDTO> updateProfile(@Valid @RequestBody UpdateProfileRequestDTO requestDTO) {
        Long userId = getCurrentUserId();
        UserDTO profile = userService.updateProfile(userId, requestDTO);
        return ResponseEntity.ok(profile);
    }

    @PostMapping("/change-password")
    public ResponseEntity<String> changePassword(@Valid @RequestBody ChangePasswordRequestDTO requestDTO) {
        Long userId = getCurrentUserId();
        userService.changePassword(userId, requestDTO);
        return ResponseEntity.ok("Password changed successfully");
    }

    @PostMapping("/admin/unlock/{userId}")
    public ResponseEntity<String> unlockAccount(@PathVariable Long userId) {
        userService.unlockAccount(userId);
        return ResponseEntity.ok("Account unlocked successfully");
    }

    @PostMapping("/admin/toggle-status/{userId}")
    public ResponseEntity<String> toggleUserStatus(
            @PathVariable Long userId,
            @RequestParam Boolean enabled) {
        userService.toggleUserStatus(userId, enabled);
        return ResponseEntity.ok("User status updated successfully");
    }

    // FIXED: Now principal is userId (Long)
    private Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication != null && authentication.getPrincipal() instanceof Long) {
            return (Long) authentication.getPrincipal();
        }

        throw new IllegalStateException("User not authenticated");
    }

    private UserDTO mapToUserDTO(User user) {
        return UserDTO.builder()
                .id(user.getId())
                .phoneNumber(user.getPhoneNumber())
                .fullName(user.getFullName())
                .profileImageUrl(user.getProfileImageUrl())
                .role(user.getRole())
                .enabled(user.getEnabled())
                .createdAt(user.getCreatedAt())
                .lastLogin(user.getLastLogin())
                .build();
    }
}