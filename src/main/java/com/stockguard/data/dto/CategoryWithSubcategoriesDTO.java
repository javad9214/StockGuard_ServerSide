package com.stockguard.data.dto;

import com.stockguard.data.entity.Category;
import com.stockguard.data.entity.Subcategory;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * A category together with its live subcategories, served to clients so
 * users can pick a subcategory when creating or editing a product.
 */
@Getter
@Builder
public class CategoryWithSubcategoriesDTO {

    private Integer id;
    private String name;
    private String icon; // optional
    private List<SubcategoryDTO> subcategories; // empty when the category has none

    public static CategoryWithSubcategoriesDTO from(Category c, List<Subcategory> subcategories) {
        return CategoryWithSubcategoriesDTO.builder()
                .id(c.getId())
                .name(c.getName())
                .icon(c.getIcon())
                .subcategories(subcategories.stream().map(SubcategoryDTO::from).toList())
                .build();
    }
}
