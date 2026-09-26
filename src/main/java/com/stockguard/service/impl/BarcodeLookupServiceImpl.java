package com.stockguard.service.impl;

import com.stockguard.client.DaryamartClient;
import com.stockguard.data.dto.barcode.response.BarcodeProductResponseDTO;
import com.stockguard.data.dto.daryamart.DaryamartProductDto;
import com.stockguard.data.dto.daryamart.DaryamartSearchResponseDto;
import com.stockguard.data.entity.CatalogProduct;
import com.stockguard.data.entity.Category;
import com.stockguard.data.entity.Subcategory;
import com.stockguard.repository.CatalogProductRepository;
import com.stockguard.repository.CategoryRepository;
import com.stockguard.repository.SubcategoryRepository;
import com.stockguard.service.BarcodeLookupService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class BarcodeLookupServiceImpl implements BarcodeLookupService {

    private static final int PAGE_NUMBER = 1;
    private static final int PAGE_SIZE = 50;

    private static final String EXTERNAL_SOURCE = "DARYAMART";
    private static final String UNKNOWN_CATEGORY = "Unknown";

    private static final BigDecimal TOMAN_TO_RIAL = BigDecimal.TEN;

    private final DaryamartClient daryamartClient;
    private final CatalogProductRepository catalogProductRepository;
    private final CategoryRepository categoryRepository;
    private final SubcategoryRepository subcategoryRepository;

    @Value("${daryamart.api.base-url}")
    private String baseUrl;

    @Override
    public Optional<BarcodeProductResponseDTO> lookupByBarcode(String barcode) {
        Optional<CatalogProduct> local = catalogProductRepository.findByBarcodeAndIsActiveTrue(barcode);
        if (local.isPresent()) {
            log.info("🏷️ Barcode found in catalog: {}", barcode);
            return local.map(this::toResponse);
        }

        log.info("🏷️ Barcode not in catalog, falling back to Daryamart: {}", barcode);
        return lookupInDaryamart(barcode);
    }

    private Optional<BarcodeProductResponseDTO> lookupInDaryamart(String barcode) {

        DaryamartSearchResponseDto response =
                daryamartClient.searchProducts(barcode, PAGE_NUMBER, PAGE_SIZE);

        if (response == null || !Boolean.TRUE.equals(response.getSuccess())
                || response.getData() == null) {
            log.warn("⚠️ Daryamart search failed for barcode {}: {}",
                    barcode, response != null ? response.getMessage() : "null response");
            return Optional.empty();
        }

        List<DaryamartProductDto> products = response.getData().getSource();
        if (products == null || products.isEmpty()) {
            log.info("🔍 No Daryamart product found for barcode: {}", barcode);
            return Optional.empty();
        }

        DaryamartProductDto product = products.get(0);
        saveToCatalog(barcode, product);
        return Optional.of(toResponse(product));
    }

    private void saveToCatalog(String barcode, DaryamartProductDto product) {
        if (product.getId() == null) {
            log.warn("⚠️ Daryamart product has no id, not saving to catalog: {}", barcode);
            return;
        }

        try {
            if (catalogProductRepository.existsByExternalSourceAndExternalSourceId(
                    EXTERNAL_SOURCE, product.getId())) {
                log.debug("⏭ Daryamart product already in catalog: id={}", product.getId());
                return;
            }

            CatalogProduct saved = catalogProductRepository.save(CatalogProduct.builder()
                    .name(StringUtils.hasText(product.getName()) ? product.getName() : "بدون نام")
                    .barcode(barcode)
                    // external CDN image — stored as the full URL (only our own
                    // uploads are MinIO keys); resolved at read time
                    .imageKey(toAbsoluteImageUrl(product.getImageAddress()))
                    .suggestedSellPrice(toRial(product.getPrice()))
                    .externalSource(EXTERNAL_SOURCE)
                    .externalSourceId(product.getId())
                    .imageSource(EXTERNAL_SOURCE)
                    .subcategory(resolveUnknownSubcategory())
                    .status(CatalogProduct.CatalogStatus.VERIFIED)
                    .qualityScore(70)
                    .adoptionCount(0)
                    .isActive(true)
                    .build());

            log.info("✅ Saved Daryamart product to catalog: barcode={}, name={}, catalogId={}",
                    barcode, saved.getName(), saved.getId());
        } catch (Exception e) {
            // the lookup response must still be returned even if caching fails
            // (e.g. concurrent save of the same barcode)
            log.warn("⚠️ Could not save Daryamart product to catalog: barcode={}, reason={}",
                    barcode, e.getMessage());
        }
    }

    private Subcategory resolveUnknownSubcategory() {
        Category category = categoryRepository.findByName(UNKNOWN_CATEGORY)
                .orElseGet(() -> categoryRepository.save(
                        Category.builder().name(UNKNOWN_CATEGORY).build()));

        return subcategoryRepository.findByNameAndCategory(UNKNOWN_CATEGORY, category)
                .orElseGet(() -> subcategoryRepository.save(
                        Subcategory.builder()
                                .name(UNKNOWN_CATEGORY)
                                .category(category)
                                .build()));
    }

    private BarcodeProductResponseDTO toResponse(DaryamartProductDto product) {
        return BarcodeProductResponseDTO.builder()
                .name(product.getName())
                .imageUrl(toAbsoluteImageUrl(product.getImageAddress()))
                .sellPrice(toRial(product.getPrice()))
                .build();
    }

    private BarcodeProductResponseDTO toResponse(CatalogProduct product) {
        return BarcodeProductResponseDTO.builder()
                .name(product.getName())
                .imageUrl(product.getImageUrl())
                .sellPrice(product.getSuggestedSellPrice())
                .costPrice(product.getSuggestedCostPrice())
                .build();
    }

    private String toAbsoluteImageUrl(String imageAddress) {
        if (!StringUtils.hasText(imageAddress)) {
            return null;
        }
        if (imageAddress.startsWith("http://") || imageAddress.startsWith("https://")) {
            return imageAddress;
        }
        return baseUrl + imageAddress;
    }

    private Long toRial(Long tomanPrice) {
        return tomanPrice == null ? null : tomanPrice * 10;
    }
}
