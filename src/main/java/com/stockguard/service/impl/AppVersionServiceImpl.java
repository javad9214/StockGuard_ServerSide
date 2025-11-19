package com.stockguard.service.impl;



import com.stockguard.data.dto.appversion.AppVersionRequestDTO;
import com.stockguard.data.dto.appversion.AppVersionResponseDTO;
import com.stockguard.data.entity.AppVersion;
import com.stockguard.repository.AppVersionRepository;
import com.stockguard.service.AppVersionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AppVersionServiceImpl implements AppVersionService {

    private final AppVersionRepository appVersionRepository;

    @Transactional
    @Override
    public AppVersionResponseDTO createVersion(AppVersionRequestDTO requestDTO) {
        if (appVersionRepository.existsByPlatform(requestDTO.getPlatform())) {
            throw new IllegalArgumentException("Version configuration for platform " +
                    requestDTO.getPlatform() + " already exists. Use update instead.");
        }
        if (requestDTO.getLastVersionCode() < requestDTO.getMinVersionCode()) {
            throw new IllegalArgumentException("Last version code must be greater than or equal to minimum version code");
        }
        AppVersion appVersion = mapToEntity(requestDTO);
        AppVersion savedVersion = appVersionRepository.save(appVersion);
        return mapToResponseDTO(savedVersion);
    }

    @Transactional
    @Override
    public AppVersionResponseDTO updateVersion(String platform, AppVersionRequestDTO requestDTO) {
        AppVersion appVersion = appVersionRepository.findByPlatform(platform)
                .orElseThrow(() -> new IllegalArgumentException("Version configuration not found for platform: " + platform));
        if (requestDTO.getLastVersionCode() < requestDTO.getMinVersionCode()) {
            throw new IllegalArgumentException("Last version code must be greater than or equal to minimum version code");
        }
        copyFields(appVersion, requestDTO);
        AppVersion updatedVersion = appVersionRepository.save(appVersion);
        return mapToResponseDTO(updatedVersion);
    }

    @Transactional(readOnly = true)
    @Override
    public AppVersionResponseDTO getVersionByPlatform(String platform) {
        AppVersion appVersion = appVersionRepository.findByPlatform(platform)
                .orElseThrow(() -> new IllegalArgumentException("Version configuration not found for platform: " + platform));
        return mapToResponseDTO(appVersion);
    }

    @Transactional(readOnly = true)
    @Override
    public List<AppVersionResponseDTO> getAllVersions() {
        return appVersionRepository.findAll().stream()
                .map(this::mapToResponseDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    @Override
    public void deleteVersion(String platform) {
        AppVersion appVersion = appVersionRepository.findByPlatform(platform)
                .orElseThrow(() -> new IllegalArgumentException("Version configuration not found for platform: " + platform));
        appVersionRepository.delete(appVersion);
    }


    private AppVersion mapToEntity(AppVersionRequestDTO dto) {
        AppVersion appVersion = new AppVersion();
        copyFields(appVersion, dto);
        return appVersion;
    }

    private AppVersionResponseDTO mapToResponseDTO(AppVersion entity) {
        return AppVersionResponseDTO.builder()
                .id(entity.getId())
                .platform(entity.getPlatform())
                .minVersionCode(entity.getMinVersionCode())
                .lastVersionCode(entity.getLastVersionCode())
                .minVersionName(entity.getMinVersionName())
                .lastVersionName(entity.getLastVersionName())
                .updateUrl(entity.getUpdateUrl())
                .releaseNotes(entity.getReleaseNotes())
                .enabled(entity.getEnabled())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    private void copyFields(AppVersion appVersion, AppVersionRequestDTO dto) {
        appVersion.setPlatform(dto.getPlatform());
        appVersion.setMinVersionCode(dto.getMinVersionCode());
        appVersion.setLastVersionCode(dto.getLastVersionCode());
        appVersion.setMinVersionName(dto.getMinVersionName());
        appVersion.setLastVersionName(dto.getLastVersionName());
        appVersion.setUpdateUrl(dto.getUpdateUrl());
        appVersion.setReleaseNotes(dto.getReleaseNotes());
        appVersion.setEnabled(dto.getEnabled());
    }

}

