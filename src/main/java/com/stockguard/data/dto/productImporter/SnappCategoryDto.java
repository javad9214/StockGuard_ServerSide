package com.stockguard.data.dto.productImporter;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class SnappCategoryDto {

    @JsonProperty("category_id")
    private Long id;

    @JsonProperty("category_title")
    private String title;

    private List<SnappProductDto> products;
}
