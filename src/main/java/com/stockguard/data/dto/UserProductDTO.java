package com.stockguard.data.dto;


import com.stockguard.data.enums.Unit;
import lombok.Data;

@Data
public class UserProductDTO {
    private Long catalogProductId; // null = custom product
    private String barcode;
    private String customName;
    private Long price;
    private Long costPrice;
    private String description;
    private Integer subcategoryId;
    private Integer supplierId;
    private Unit unit; // enum name only (e.g. KILOGRAM); anything else is rejected with 400
    private Integer stock;
    private Integer minStockLevel;
    private Integer maxStockLevel;
    private Boolean isActive;
    private String tags;
}