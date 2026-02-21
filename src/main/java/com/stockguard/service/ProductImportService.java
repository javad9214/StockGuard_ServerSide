package com.stockguard.service;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.stockguard.data.dto.productImporter.*;
import com.stockguard.data.entity.Category;
import com.stockguard.data.entity.CatalogProduct;
import com.stockguard.data.entity.Subcategory;
import com.stockguard.data.dto.ProductImportDto;
import com.stockguard.repository.CatalogProductRepository;
import com.stockguard.repository.CategoryRepository;
import com.stockguard.repository.SubcategoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductImportService {

    private final ObjectMapper objectMapper;
    private final CategoryRepository categoryRepository;
    private final SubcategoryRepository subcategoryRepository;
    private final CatalogProductRepository catalogProductRepository;

    // ---------------- NORMAL JSON IMPORT ----------------

    @Transactional
    public void importFromJson(String fileName) throws Exception {
        ClassPathResource resource = new ClassPathResource("data/" + fileName);
        InputStream inputStream = resource.getInputStream();

        List<ProductImportDto> dtos = objectMapper.readValue(
                inputStream,
                new com.fasterxml.jackson.core.type.TypeReference<List<ProductImportDto>>() {}
        );

        log.info("Total products found in JSON: {}", dtos.size());

        int imported = 0, skipped = 0;

        for (ProductImportDto dto : dtos) {

            if (catalogProductRepository.existsByExternalSourceAndExternalSourceId(
                    "LOCAL", Long.valueOf(dto.getBarcode()))) {
                log.debug("Duplicate skipped: barcode={}, name='{}'", dto.getBarcode(), dto.getName());
                skipped++;
                continue;
            }

            String catName = (dto.getCategory() == null || dto.getCategory().isBlank())
                    ? "Unknown" : dto.getCategory();

            Category category = categoryRepository.findByName(catName)
                    .orElseGet(() -> categoryRepository.save(
                            Category.builder().name(catName).build()
                    ));

            String subName = (dto.getSubcategory() == null || dto.getSubcategory().isBlank())
                    ? "Unknown" : dto.getSubcategory();

            Subcategory subcategory = subcategoryRepository
                    .findByNameAndCategory(subName, category)
                    .orElseGet(() -> subcategoryRepository.save(
                            Subcategory.builder().name(subName).category(category).build()
                    ));

            CatalogProduct product = CatalogProduct.builder()
                    .name(dto.getName() != null ? dto.getName() : "بدون نام")
                    .barcode(dto.getBarcode())
                    .subcategory(subcategory)
                    .externalSource("LOCAL")
                    .externalSourceId(Long.valueOf(dto.getBarcode()))
                    .status(CatalogProduct.CatalogStatus.VERIFIED)
                    .isActive(true)
                    .qualityScore(70)
                    .adoptionCount(0)
                    .imageSource("LOCAL")
                    .build();

            catalogProductRepository.save(product);
            log.info("Imported: barcode={}, name='{}'", dto.getBarcode(), dto.getName());
            imported++;
        }

        log.info("Done — total: {}, imported: {}, skipped: {}", dtos.size(), imported, skipped);
    }


    // ---------------- SNAPP IMPORT ----------------

    @Transactional
    public void importFromSnapp(InputStream inputStream) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

            List<SnappProductDto> products = mapper.readValue(
                    inputStream,
                    new com.fasterxml.jackson.core.type.TypeReference<List<SnappProductDto>>() {}
            );

            log.info("Total products found in JSON: {}", products.size());

            Category category = categoryRepository.findByName("Snapp")
                    .orElseGet(() -> categoryRepository.save(
                            Category.builder().name("Snapp").build()
                    ));

            Subcategory subcategory = subcategoryRepository
                    .findByNameAndCategory("Snapp", category)
                    .orElseGet(() -> subcategoryRepository.save(
                            Subcategory.builder().name("Snapp").category(category).build()
                    ));

            int imported = 0, skipped = 0;

            for (SnappProductDto dto : products) {
                if (catalogProductRepository.existsByExternalSourceAndExternalSourceId(
                        "SNAPP_MARKET", dto.getId())) {
                    log.debug("Duplicate skipped: id={}, title='{}'", dto.getId(), dto.getTitle());
                    skipped++;
                    continue;
                }

                catalogProductRepository.save(mapToCatalogProduct(dto, subcategory));
                log.info("Imported: id={}, title='{}'", dto.getId(), dto.getTitle());
                imported++;
            }

            log.info("Done — total: {}, imported: {}, skipped: {}", products.size(), imported, skipped);

        } catch (Exception e) {
            log.error("SNAPP IMPORT FAILED", e);
            throw new RuntimeException("Snapp import failed", e);
        }
    }



    // ---------------- MAPPER ----------------
    private CatalogProduct mapToCatalogProduct(SnappProductDto dto, Subcategory subcategory) {
        return CatalogProduct.builder()
                .name(dto.getTitle() != null ? dto.getTitle() : "بدون نام")
                .externalSource("SNAPP_MARKET")
                .externalSourceId(dto.getId())
                .suggestedPrice(dto.getFinalPrice() != null ? dto.getFinalPrice() : dto.getPrice())
                .brand(dto.getBrand())
                .imageUrl(dto.getImageUrl())
                .imageSource("SNAPP_MARKET")
                .subcategory(subcategory)
                .status(CatalogProduct.CatalogStatus.VERIFIED)
                .qualityScore(70)
                .adoptionCount(0)
                .isActive(true)
                .build();
    }

}
