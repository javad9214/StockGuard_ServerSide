package com.stockguard.data.dto.productImporter;

import lombok.Data;

import java.util.List;

@Data
public class SnappRootDto {
    private List<SnappItemDto> items;
}
