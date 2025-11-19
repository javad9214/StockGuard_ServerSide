package com.stockguard.data.dto.appversion;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AppVersionResponseDTO {

    private Long id;
    private String platform;
    private Integer minVersionCode;
    private Integer lastVersionCode;
    private String minVersionName;
    private String lastVersionName;
    private String updateUrl;
    private String releaseNotes;
    private Boolean enabled;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
