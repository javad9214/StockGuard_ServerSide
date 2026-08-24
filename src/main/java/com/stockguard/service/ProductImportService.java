package com.stockguard.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.stockguard.data.dto.productImporter.*;
import com.stockguard.data.entity.Category;
import com.stockguard.data.entity.CatalogProduct;
import com.stockguard.data.entity.Subcategory;
import com.stockguard.data.dto.ProductImportDTO;
import com.stockguard.repository.CatalogProductRepository;
import com.stockguard.repository.CategoryRepository;
import com.stockguard.repository.SubcategoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductImportService {

    private final ObjectMapper objectMapper;
    private final ProductImportTxService txService;
    private final CategoryRepository categoryRepository;
    private final SubcategoryRepository subcategoryRepository;
    private final CatalogProductRepository catalogProductRepository;

    // ---------------- NORMAL JSON IMPORT ----------------

    public void importFromJson(String fileName) throws Exception {

        List<ProductImportDTO> dtos = loadDtos(fileName);

        int imported = 0;
        int failed = 0;

        for (ProductImportDTO dto : dtos) {
            try {
                txService.importSingle(dto); // ✅ از Proxy رد می‌شود
                imported++;
            } catch (Exception e) {
                failed++;
                log.error("❌ Import failed for barcode={}",
                        dto.getBarcode(), e);
            }
        }

        log.info("🏁 Import finished. total={}, imported={}, failed={}",
                dtos.size(), imported, failed);
    }

    private List<ProductImportDTO> loadDtos(String fileName) throws IOException {
        try (InputStream is =
                     new ClassPathResource("data/" + fileName).getInputStream()) {
            return objectMapper.readValue(
                    is,
                    new TypeReference<>() {
                    }
            );
        }
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
