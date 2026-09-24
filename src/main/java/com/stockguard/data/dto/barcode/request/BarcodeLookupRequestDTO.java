package com.stockguard.data.dto.barcode.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class BarcodeLookupRequestDTO {

    @Schema(title = "product barcode", example = "6261145000407")
    @NotBlank(message = "barcode must not be blank")
    private String barcode;
}
