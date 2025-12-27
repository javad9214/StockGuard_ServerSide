package com.stockguard.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.stockguard.data.entity.Category;
import com.stockguard.data.entity.Subcategory;
import com.stockguard.data.dto.ProductImportDto;
import com.stockguard.data.entity.UserProduct;
import com.stockguard.repository.CategoryRepository;
import com.stockguard.repository.SubcategoryRepository;
import com.stockguard.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductImportService {

    private final CategoryRepository categoryRepository;
    private final SubcategoryRepository subcategoryRepository;
    private final ProductRepository productRepository;

    @Transactional
    public void importFromJson(String fileName) throws Exception {
        ObjectMapper mapper = new ObjectMapper();

        // Read from classpath (resources folder)
        ClassPathResource resource = new ClassPathResource("data/" + fileName);
        InputStream inputStream = resource.getInputStream();

        List<ProductImportDto> dtos = mapper.readValue(
                inputStream,
                new TypeReference<List<ProductImportDto>>() {}
        );

        List<UserProduct> productsToSave = new ArrayList<>();

        for (ProductImportDto dto : dtos) {

            String rawCatName = dto.getCategory();
            final String catName = (rawCatName == null || rawCatName.isBlank()) ? "Unknown" : rawCatName;

            // Category
            Category category = categoryRepository.findByName(catName)
                    .orElseGet(() -> categoryRepository.save(
                            Category.builder().name(catName).build()
                    ));

            String rawName = dto.getSubcategory();
            final String subcategoryName = (rawName == null || rawName.isBlank()) ? "Unknown" : rawName;

            // Subcategory
            Subcategory subcategory = subcategoryRepository
                    .findByNameAndCategory(subcategoryName, category)
                    .orElseGet(() -> subcategoryRepository.save(
                            Subcategory.builder()
                                    .name(subcategoryName)
                                    .category(category)
                                    .build()
                    ));

            // Product
            if (productRepository.findByBarcode(dto.getBarcode()).isEmpty()) {
                UserProduct product = UserProduct.builder()
                        .name(dto.getName())
                        .barcode(dto.getBarcode())
                        .subcategoryId(subcategory.getId())
                        .build();
                productsToSave.add(product);
            }
        }

        // Bulk save all products at once
        productRepository.saveAll(productsToSave);
    }
}