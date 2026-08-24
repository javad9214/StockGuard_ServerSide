package com.stockguard.service;

import com.stockguard.data.dto.ProductImportDTO;
import com.stockguard.data.entity.CatalogProduct;
import com.stockguard.data.entity.Category;
import com.stockguard.data.entity.Subcategory;
import com.stockguard.repository.CatalogProductRepository;
import com.stockguard.repository.CategoryRepository;
import com.stockguard.repository.SubcategoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductImportTxService {

    private final CatalogProductRepository catalogProductRepository;
    private final CategoryRepository categoryRepository;
    private final SubcategoryRepository subcategoryRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void importSingle(ProductImportDTO dto) {

        String cleanBarcode = normalizeBarcode(dto.getBarcode());
        Long externalId = Long.parseLong(cleanBarcode);

        if (catalogProductRepository
                .existsByExternalSourceAndExternalSourceId("LOCAL", externalId)) {
            log.debug("⏭ Skipped duplicate barcode={}", cleanBarcode);
            return;
        }

        Category category = categoryRepository
                .findByName(normalizeName(dto.getCategory(), "Unknown"))
                .orElseGet(() ->
                        categoryRepository.save(
                                Category.builder()
                                        .name(normalizeName(dto.getCategory(), "Unknown"))
                                        .build()
                        )
                );

        Subcategory subcategory = subcategoryRepository
                .findByNameAndCategory(
                        normalizeName(dto.getSubcategory(), "Unknown"),
                        category
                )
                .orElseGet(() ->
                        subcategoryRepository.save(
                                Subcategory.builder()
                                        .name(normalizeName(dto.getSubcategory(), "Unknown"))
                                        .category(category)
                                        .build()
                        )
                );

        CatalogProduct product = CatalogProduct.builder()
                .name(dto.getName() != null ? dto.getName() : "بدون نام")
                .barcode(cleanBarcode)
                .externalSource("LOCAL")
                .externalSourceId(externalId)
                .subcategory(subcategory)
                .status(CatalogProduct.CatalogStatus.VERIFIED)
                .isActive(true)
                .qualityScore(70)
                .adoptionCount(0)
                .imageSource("LOCAL")
                .build();

        catalogProductRepository.save(product);

        log.info("✅ COMMITTED product barcode={}", cleanBarcode);
    }

    // ---------------- helpers ----------------

    private String normalizeBarcode(String barcode) {
        if (barcode == null) {
            throw new IllegalArgumentException("Barcode is null");
        }
        String clean = barcode.replaceAll("\\D", "");
        if (clean.isBlank()) {
            throw new IllegalArgumentException("Invalid barcode: " + barcode);
        }
        return clean;
    }

    private String normalizeName(String value, String def) {
        return (value == null || value.isBlank()) ? def : value.trim();
    }
}

