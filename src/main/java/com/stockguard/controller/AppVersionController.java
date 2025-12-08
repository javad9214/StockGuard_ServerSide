package com.stockguard.controller;

import com.stockguard.data.dto.appversion.AppVersionRequestDTO;
import com.stockguard.data.dto.appversion.AppVersionResponseDTO;
import com.stockguard.service.AppVersionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/version")
@RequiredArgsConstructor
@Slf4j
public class AppVersionController {

    private final AppVersionService appVersionService;

    // ============================================
    // PUBLIC ENDPOINTS - No authentication required
    // ============================================

    /**
     * PUBLIC: Check version for Android platform
     * GET /api/version/android
     */
    @GetMapping("/android")
    public ResponseEntity<AppVersionResponseDTO> getAndroidVersion() {
        log.info("GET /api/version/android called"); // <-- log entry

        AppVersionResponseDTO response = appVersionService.getVersionByPlatform("ANDROID");

        log.info("GET /api/version/android response: {}", response); // <-- log response
        return ResponseEntity.ok(response);
    }

    /**
     * PUBLIC: Check version for iOS platform
     * GET /api/version/ios
     */
    @GetMapping("/ios")
    public ResponseEntity<AppVersionResponseDTO> getIosVersion() {
        AppVersionResponseDTO response = appVersionService.getVersionByPlatform("IOS");
        return ResponseEntity.ok(response);
    }

    /**
     * PUBLIC: Generic version check endpoint
     * GET /api/version/check?platform=ANDROID
     */
    @GetMapping("/check")
    public ResponseEntity<AppVersionResponseDTO> checkVersion(
            @RequestParam(defaultValue = "ANDROID") String platform) {
        AppVersionResponseDTO response = appVersionService.getVersionByPlatform(platform.toUpperCase());
        return ResponseEntity.ok(response);
    }

    // ============================================
    // ADMIN ENDPOINTS - Requires ADMIN role
    // ============================================

    /**
     * ADMIN: Create new version configuration
     * POST /api/version/admin
     */
    @PostMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AppVersionResponseDTO> createVersion(
            @Valid @RequestBody AppVersionRequestDTO requestDTO) {
        AppVersionResponseDTO response = appVersionService.createVersion(requestDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * ADMIN: Update existing version configuration
     * PUT /api/version/admin/{platform}
     */
    @PutMapping("/admin/{platform}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AppVersionResponseDTO> updateVersion(
            @PathVariable String platform,
            @Valid @RequestBody AppVersionRequestDTO requestDTO) {
        AppVersionResponseDTO response = appVersionService.updateVersion(platform.toUpperCase(), requestDTO);
        return ResponseEntity.ok(response);
    }

    /**
     * ADMIN: Get version configuration for a specific platform
     * GET /api/version/admin/{platform}
     */
    @GetMapping("/admin/{platform}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AppVersionResponseDTO> getVersionByPlatformAdmin(
            @PathVariable String platform) {
        AppVersionResponseDTO response = appVersionService.getVersionByPlatform(platform.toUpperCase());
        return ResponseEntity.ok(response);
    }

    /**
     * ADMIN: Get all version configurations
     * GET /api/version/admin/all
     */
    @GetMapping("/admin/all")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<AppVersionResponseDTO>> getAllVersions() {
        List<AppVersionResponseDTO> response = appVersionService.getAllVersions();
        return ResponseEntity.ok(response);
    }

    /**
     * ADMIN: Delete version configuration
     * DELETE /api/version/admin/{platform}
     */
    @DeleteMapping("/admin/{platform}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteVersion(@PathVariable String platform) {
        appVersionService.deleteVersion(platform.toUpperCase());
        return ResponseEntity.noContent().build();
    }
}