package com.stockguard.data.dto.productImporter;

import lombok.Data;

import java.util.List;

@Data
public class SnappItemDto {
    private Long id;
    private String title; // category / subcategory title
    private List<SnappProductDto> products;
}
