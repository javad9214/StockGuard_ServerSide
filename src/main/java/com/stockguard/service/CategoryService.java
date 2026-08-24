package com.stockguard.service;

import com.stockguard.data.dto.CategoryWithSubcategoriesDTO;

import java.util.List;

public interface CategoryService {

    List<CategoryWithSubcategoriesDTO> getCategoriesWithSubcategories();
}
