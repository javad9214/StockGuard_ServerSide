package com.stockguard.data.dto.appversion;


import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AppVersionRequestDTO {

    @NotBlank(message = "Platform is required")
    @Pattern(regexp = "ANDROID|IOS", message = "Platform must be ANDROID or IOS")
    private String platform;

    @NotNull(message = "Minimum version code is required")
    @Min(value = 1, message = "Minimum version code must be at least 1")
    private Integer minVersionCode;

    @NotNull(message = "Last version code is required")
    @Min(value = 1, message = "Last version code must be at least 1")
    private Integer lastVersionCode;

    @NotBlank(message = "Minimum version name is required")
    private String minVersionName;

    @NotBlank(message = "Last version name is required")
    private String lastVersionName;

    private String updateUrl;

    private String releaseNotes;


    @NotNull(message = "Enabled flag is required")
    private Boolean enabled;
}