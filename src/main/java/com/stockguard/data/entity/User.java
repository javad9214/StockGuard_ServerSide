package com.stockguard.data.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "users", indexes = {
        @Index(name = "idx_phone_number", columnList = "phoneNumber"),
        @Index(name = "idx_enabled", columnList = "enabled")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 20)
    private String phoneNumber;

    @Column(nullable = false)
    private String password; // BCrypt hashed

    @Column(nullable = false, length = 100)
    private String fullName;

    @Column(nullable = false)
    private Boolean enabled = true;

    @Column(nullable = false)
    private Boolean accountLocked = false; // For security - lock after failed attempts

    @Column(nullable = false)
    private Integer failedLoginAttempts = 0; // Track failed login attempts

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserRole role = UserRole.USER;

    private String profileImageUrl; // Optional profile picture

    private String deviceToken; // For push notifications (FCM token)

    @Column(length = 50)
    private String deviceId; // Track user's device

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    private LocalDateTime lastLogin;

    private LocalDateTime accountLockedAt; // When account was locked

    // Enum for roles
    public enum UserRole {
        USER,
        ADMIN,
        PREMIUM_USER // If you have premium features
    }
}