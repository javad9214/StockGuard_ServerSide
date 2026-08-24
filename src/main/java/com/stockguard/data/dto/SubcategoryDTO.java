package com.stockguard.data.dto;

import com.stockguard.data.entity.Subcategory;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class SubcategoryDTO {

    private Integer id;
    private String name;
    private String icon; // optional

    public static SubcategoryDTO from(Subcategory s) {
        return SubcategoryDTO.builder()
                .id(s.getId())
                .name(s.getName())
                .icon(s.getIcon())
                .build();
    }
}
