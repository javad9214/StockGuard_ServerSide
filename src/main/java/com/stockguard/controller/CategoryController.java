package com.stockguard.controller;

import com.stockguard.data.dto.category.response.CategoryWithSubcategoriesDTO;
import com.stockguard.data.dto.common.ResponseDTO;
import com.stockguard.service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
public class CategoryController extends BaseController {

    private final CategoryService categoryService;

    /**
     * All live categories with their subcategories, for the client's
     * category/subcategory pickers.
     * GET /api/categories
     */
    @GetMapping
    public ResponseEntity<ResponseDTO<List<CategoryWithSubcategoriesDTO>>> getCategories() {
        return generateOKResponse(categoryService.getCategoriesWithSubcategories());
    }
}
