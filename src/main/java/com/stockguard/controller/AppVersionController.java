package com.stockguard.controller;


import com.stockguard.data.dto.appversion.AppVersionRequestDTO;
import com.stockguard.data.dto.appversion.AppVersionResponseDTO;
import com.stockguard.service.AppVersionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/version")
@RequiredArgsConstructor
public class AppVersionController {

    private final AppVersionService appVersionService;


    /**
     * Admin endpoint to create new version configuration
     * POST /api/version/admin
     */
    @PostMapping("/admin")
    public ResponseEntity<AppVersionResponseDTO> createVersion(
            @Valid @RequestBody AppVersionRequestDTO requestDTO) {
        AppVersionResponseDTO response = appVersionService.createVersion(requestDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Admin endpoint to update existing version configuration
     * PUT /api/version/admin/{platform}
     */
    @PutMapping("/admin/{platform}")
    public ResponseEntity<AppVersionResponseDTO> updateVersion(
            @PathVariable String platform,
            @Valid @RequestBody AppVersionRequestDTO requestDTO) {
        AppVersionResponseDTO response = appVersionService.updateVersion(platform, requestDTO);
        return ResponseEntity.ok(response);
    }

    /**
     * Admin endpoint to get version configuration for a specific platform
     * GET /api/version/admin/{platform}
     */
    @GetMapping("/admin/{platform}")
    public ResponseEntity<AppVersionResponseDTO> getVersionByPlatform(
            @PathVariable String platform) {
        AppVersionResponseDTO response = appVersionService.getVersionByPlatform(platform);
        return ResponseEntity.ok(response);
    }

    /**
     * Admin endpoint to get all version configurations
     * GET /api/version/admin/all
     */
    @GetMapping("/admin/all")
    public ResponseEntity<List<AppVersionResponseDTO>> getAllVersions() {
        List<AppVersionResponseDTO> response = appVersionService.getAllVersions();
        return ResponseEntity.ok(response);
    }

    /**
     * Admin endpoint to delete version configuration
     * DELETE /api/version/admin/{platform}
     */
    @DeleteMapping("/admin/{platform}")
    public ResponseEntity<Void> deleteVersion(@PathVariable String platform) {
        appVersionService.deleteVersion(platform);
        return ResponseEntity.noContent().build();
    }
}
