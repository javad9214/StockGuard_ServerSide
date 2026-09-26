package com.stockguard.data.dto.image.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ImageUploadResponseDTO {

    /** Object key under which the image is stored — the only value to persist (CatalogProduct.imageKey) */
    private String key;

    /** Absolute URL the image is served from, built from app.base-url — for display only, never persisted */
    private String url;
}
