package com.stockguard.data.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "app_versions")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AppVersion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 20)
    private String platform; // "ANDROID" or "IOS"

    @Column(nullable = false)
    private Integer minVersionCode;

    @Column(nullable = false)
    private Integer lastVersionCode;

    @Column(nullable = false, length = 20)
    private String minVersionName;

    @Column(nullable = false, length = 20)
    private String lastVersionName;

    @Column(length = 500)
    private String updateUrl; // Play Store or custom URL

    @Column(columnDefinition = "TEXT")
    private String releaseNotes; // What's new in the latest version


    @Column(nullable = false)
    private Boolean enabled = true; // Enable/disable version checking

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;
}