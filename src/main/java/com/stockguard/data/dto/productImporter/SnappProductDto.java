package com.stockguard.data.dto.productImporter;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class SnappProductDto {

    @JsonProperty("id_snapp")
    private Long id;

    private String title;

    @JsonProperty("pure_title")
    private String pureTitle;

    private String brand;

    @JsonProperty("brand_en")
    private String brandEn;

    private Long price;

    @JsonProperty("final_price")
    private Long finalPrice;

    private Integer discount;

    @JsonProperty("image_url")
    private String imageUrl;

    private String barcode;

    @JsonProperty("category_title")
    private String categoryTitle;

    @JsonProperty("category_id")
    private Long categoryId;
}
