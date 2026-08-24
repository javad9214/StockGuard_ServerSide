package com.stockguard.data.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ProductImportDTO {
    private String name;
    private String  barcode;
    private String category;
    private String subcategory;
}