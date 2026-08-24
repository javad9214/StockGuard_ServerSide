package com.stockguard.data.dto;

import com.stockguard.data.entity.CatalogProduct;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
@Getter
@Builder
public class CatalogProductResponseDTO {

    private Long id;
    private String name;
    private String barcode;
    private String description;
    private String brand;
    private String manufacturer;

    // Category
    private Long subcategoryId;
    private String subcategoryName;
    private String categoryName;

    // Image
    private String imageUrl;
    private String imageSource;

    // Price & Unit
    private Long suggestedPrice;
    private String unit;

    // Meta
    private String tags;
    private CatalogProduct.CatalogStatus status;

    // Stats
    private Integer qualityScore;
    private Integer adoptionCount;

    // External
    private String externalSource;
    private Long externalSourceId;

    // Lifecycle
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Mapper
    public static CatalogProductResponseDTO from(CatalogProduct p) {
        return CatalogProductResponseDTO.builder()
                .id(p.getId())
                .name(p.getName())
                .barcode(p.getBarcode())
                .description(p.getDescription())
                .brand(p.getBrand())
                .manufacturer(p.getManufacturer())
                .subcategoryId(p.getSubcategory() != null ? p.getSubcategory().getId().longValue() : null)
                .subcategoryName(p.getSubcategory() != null ? p.getSubcategory().getName() : null)
                .categoryName(p.getSubcategory() != null && p.getSubcategory().getCategory() != null
                        ? p.getSubcategory().getCategory().getName() : null)
                .imageUrl(p.getImageUrl())
                .imageSource(p.getImageSource())
                .suggestedPrice(p.getSuggestedPrice())
                .unit(p.getUnit())
                .tags(p.getTags())
                .status(p.getStatus())
                .qualityScore(p.getQualityScore())
                .adoptionCount(p.getAdoptionCount())
                .externalSource(p.getExternalSource())
                .externalSourceId(p.getExternalSourceId())
                .createdAt(p.getCreatedAt())
                .updatedAt(p.getUpdatedAt())
                .build();
    }
}
