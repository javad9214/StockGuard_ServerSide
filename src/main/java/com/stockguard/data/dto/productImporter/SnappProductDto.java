package com.stockguard.data.dto.productImporter;

import lombok.Data;

import java.util.List;

@Data
public class SnappProductDto {
    private Long id;
    private String title;
    private String pureTitle;
    private Integer price;
    private Integer discounted_price;
    private Integer discount_percent;
    private Integer max_order_cap;
    private List<SnappImageDto> images;
    private SnappBrandDto brand;
}
