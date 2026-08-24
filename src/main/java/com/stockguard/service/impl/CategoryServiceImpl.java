package com.stockguard.service.impl;

import com.stockguard.data.dto.CategoryWithSubcategoriesDTO;
import com.stockguard.data.entity.Subcategory;
import com.stockguard.repository.CategoryRepository;
import com.stockguard.repository.SubcategoryRepository;
import com.stockguard.service.CategoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;
    private final SubcategoryRepository subcategoryRepository;

    @Override
    @Transactional(readOnly = true)
    public List<CategoryWithSubcategoriesDTO> getCategoriesWithSubcategories() {
        // Two flat queries (categories; subcategories with fetched category) and an
        // in-memory group-by — no per-category lookups, no N+1. groupingBy keeps
        // the query's name order within each category.
        Map<Integer, List<Subcategory>> subcategoriesByCategoryId =
                subcategoryRepository.findByIsDeletedFalseWithCategory().stream()
                        .collect(Collectors.groupingBy(s -> s.getCategory().getId()));

        return categoryRepository.findByIsDeletedFalseOrderByNameAsc().stream()
                .map(c -> CategoryWithSubcategoriesDTO.from(
                        c,
                        subcategoriesByCategoryId.getOrDefault(c.getId(), List.of())
                ))
                .toList();
    }
}
