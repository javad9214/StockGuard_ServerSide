package com.stockguard.data.dto;


import lombok.Data;

@Data
public class UserProductDTO {
    private Long catalogProductId; // null = custom product
    private String customName;
    private Long price;
    private Long costPrice;
    private String description;
    private Integer subcategoryId;
    private Integer supplierId;
    private String unit;
    private Integer stock;
    private Integer minStockLevel;
    private Integer maxStockLevel;
    private Boolean isActive;
    private String tags;
}