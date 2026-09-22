package com.stockguard.service;

import com.stockguard.data.dto.category.response.CategoryWithSubcategoriesDTO;

import java.util.List;

public interface CategoryService {

    List<CategoryWithSubcategoriesDTO> getCategoriesWithSubcategories();
}
