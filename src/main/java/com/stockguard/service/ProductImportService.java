package com.stockguard.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.stockguard.data.dto.productImporter.ImportResult;
import com.stockguard.data.dto.productImporter.SnappItemDto;
import com.stockguard.data.dto.productImporter.SnappProductDto;
import com.stockguard.data.dto.productImporter.SnappRootDto;
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
import org.springframework.web.multipart.MultipartFile;

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

    @Transactional
    public void importFromJson(String fileName) throws Exception {
        ObjectMapper mapper = new ObjectMapper();

        ClassPathResource resource = new ClassPathResource("data/" + fileName);
        InputStream inputStream = resource.getInputStream();

        List<ProductImportDto> dtos = mapper.readValue(
                inputStream,
                new TypeReference<>() {
                }
        );

        List<CatalogProduct> productsToSave = new ArrayList<>();

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

            // Import to CATALOG (not user products)
            if (!catalogProductRepository.existsByBarcode(dto.getBarcode())) {
                CatalogProduct product = new CatalogProduct();
                product.setName(dto.getName());
                product.setBarcode(dto.getBarcode());
                product.setStatus(CatalogProduct.CatalogStatus.VERIFIED);
                product.setIsActive(true);
                product.setQualityScore(70); // Default import quality
                product.setAdoptionCount(0);

                productsToSave.add(product);
            }
        }

        // Bulk save to catalog
        catalogProductRepository.saveAll(productsToSave);
    }


    @Transactional
    public ImportResult importFromSnapp(MultipartFile file) {

        int created = 0;
        int skipped = 0;
        int failed = 0;

        try {
            SnappRootDto root = objectMapper.readValue(
                    file.getInputStream(),
                    SnappRootDto.class
            );

            Category category = categoryRepository
                    .findByName("Snapp Market")
                    .orElseGet(() ->
                            categoryRepository.save(
                                    Category.builder()
                                            .name("Snapp Market")
                                            .build()
                            )
                    );

            for (SnappItemDto item : root.getItems()) {

                Subcategory subcategory =
                        subcategoryRepository
                                .findByNameAndCategory(item.getTitle(), category)
                                .orElseGet(() ->
                                        subcategoryRepository.save(
                                                Subcategory.builder()
                                                        .name(item.getTitle())
                                                        .category(category)
                                                        .build()
                                        )
                                );

                for (SnappProductDto dto : item.getProducts()) {
                    try {
                        if (catalogProductRepository
                                .existsByExternalSourceAndExternalSourceId(
                                        "SNAPP_MARKET",
                                        dto.getId()
                                )) {
                            skipped++;
                            continue;
                        }

                        CatalogProduct product =
                                mapToCatalogProduct(dto, subcategory);

                        catalogProductRepository.save(product);
                        created++;

                    } catch (Exception e) {
                        failed++;
                        log.error(e.getMessage());
                    }
                }
            }

        } catch (Exception e) {
            throw new RuntimeException("Snapp import failed", e);
        }

        return new ImportResult(created, skipped, failed);
    }

    private CatalogProduct mapToCatalogProduct(
            SnappProductDto dto,
            Subcategory subcategory
    ) {

        String imageUrl =
                (dto.getImages() != null && !dto.getImages().isEmpty())
                        ? dto.getImages().get(0).getImage()
                        : null;

        return CatalogProduct.builder()
                .name(
                        dto.getPureTitle() != null
                                ? dto.getPureTitle()
                                : dto.getTitle()
                )
                .brand(
                        dto.getBrand() != null
                                ? dto.getBrand().getTitle()
                                : null
                )
                .subcategory(subcategory)
                .imageUrl(imageUrl)
                .imageSource("SNAPP_MARKET")
                .suggestedPrice(
                        dto.getDiscounted_price() != null
                                ? dto.getDiscounted_price().longValue()
                                : null
                )
                .externalSource("SNAPP_MARKET")
                .externalSourceId(dto.getId())
                .status(CatalogProduct.CatalogStatus.VERIFIED)
                .qualityScore(60)
                .adoptionCount(0)
                .isActive(true)
                .build();
    }

}

