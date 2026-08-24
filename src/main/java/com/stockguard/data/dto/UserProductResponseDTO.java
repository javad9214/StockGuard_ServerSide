package com.stockguard.data.dto;

import com.stockguard.data.entity.Category;
import com.stockguard.data.entity.Subcategory;
import com.stockguard.data.entity.UserProduct;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.Base64;

/**
 * Response DTO for user products. Mirrors the app's UserProductResponseDto
 * field-for-field so barcode and image round-trip between server and app:
 * the image is returned Base64-encoded and is decoded/persisted locally by
 * the app after every pull.
 */
@Getter
@Builder
public class UserProductResponseDTO {

    private Long id;
    private Long userId;
    private Long catalogProductId; // null = custom product
    private String barcode;
    private String customName;

    private Long price;
    private Long costPrice;
    private String description;

    private String imageType;
    private String image; // Base64-encoded image bytes

    private Integer subcategoryId;
    private Integer categoryId;
    private String subcategoryName;
    private String categoryName;
    private Integer supplierId;

    private String unit;
    private Integer stock;
    private Integer minStockLevel;
    private Integer maxStockLevel;

    private Boolean isActive;
    private String tags;

    private Boolean synced;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Mapper
    public static UserProductResponseDTO from(UserProduct p) {
        return from(p, null);
    }

    /**
     * @param subcategory resolved subcategory (with its category) used to fill
     *                    the name fields; may be null when none is linked
     */
    public static UserProductResponseDTO from(UserProduct p, Subcategory subcategory) {
        Category category = subcategory != null ? subcategory.getCategory() : null;
        return UserProductResponseDTO.builder()
                .id(p.getId())
                .userId(p.getUserId())
                .catalogProductId(p.getCatalogProduct() != null ? p.getCatalogProduct().getId() : null)
                .barcode(p.getBarcode())
                .customName(p.getCustomName())
                .price(p.getPrice())
                .costPrice(p.getCostPrice())
                .description(p.getDescription())
                .imageType(p.getImageType())
                .image(p.getImage() != null ? Base64.getEncoder().encodeToString(p.getImage()) : null)
                .subcategoryId(subcategory != null ? subcategory.getId() : p.getSubcategoryId())
                .categoryId(category != null ? category.getId() : null)
                .subcategoryName(subcategory != null ? subcategory.getName() : null)
                .categoryName(category != null ? category.getName() : null)
                .supplierId(p.getSupplierId())
                .unit(p.getUnit())
                .stock(p.getStock())
                .minStockLevel(p.getMinStockLevel())
                .maxStockLevel(p.getMaxStockLevel())
                .isActive(p.getIsActive())
                .tags(p.getTags())
                .synced(p.getSynced())
                .createdAt(p.getCreatedAt())
                .updatedAt(p.getUpdatedAt())
                .build();
    }
}
