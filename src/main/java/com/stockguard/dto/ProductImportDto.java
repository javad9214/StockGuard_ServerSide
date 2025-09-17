package com.stockguard.dto;

import lombok.Data;

@Data
public class ProductImportDto {
    private String name;
    private String barcode;
    private String category;
    private String subcategory;
}