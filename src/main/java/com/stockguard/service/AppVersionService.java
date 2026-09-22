package com.stockguard.service;

import com.stockguard.data.dto.appversion.request.AppVersionRequestDTO;
import com.stockguard.data.dto.appversion.response.AppVersionResponseDTO;

import java.util.List;

public interface AppVersionService {

    AppVersionResponseDTO createVersion(AppVersionRequestDTO requestDTO);

    AppVersionResponseDTO updateVersion(String platform, AppVersionRequestDTO requestDTO);

    AppVersionResponseDTO getVersionByPlatform(String platform);

    List<AppVersionResponseDTO> getAllVersions();

    void deleteVersion(String platform);

}
