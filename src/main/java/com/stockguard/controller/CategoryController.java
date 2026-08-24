package com.stockguard.controller;

import com.stockguard.data.dto.ApiResponse;
import com.stockguard.data.dto.CategoryWithSubcategoriesDTO;
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
public class CategoryController {

    private final CategoryService categoryService;

    /**
     * All live categories with their subcategories, for the client's
     * category/subcategory pickers.
     * GET /api/categories
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<CategoryWithSubcategoriesDTO>>> getCategories() {
        return ResponseEntity.ok(
                ApiResponse.success("Categories fetched", categoryService.getCategoriesWithSubcategories())
        );
    }
}
