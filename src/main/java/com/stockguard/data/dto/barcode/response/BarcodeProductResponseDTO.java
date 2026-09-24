package com.stockguard.data.dto.barcode.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BarcodeProductResponseDTO {

    @Schema(title = "product name")
    private String name;

    @Schema(title = "full product image url")
    private String imageUrl;

    @Schema(title = "selling price")
    private Long sellPrice;
}
