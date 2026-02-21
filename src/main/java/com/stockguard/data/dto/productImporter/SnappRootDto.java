package com.stockguard.data.dto.productImporter;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class SnappRootDto {
    private List<SnappCategoryDto> items;
}
