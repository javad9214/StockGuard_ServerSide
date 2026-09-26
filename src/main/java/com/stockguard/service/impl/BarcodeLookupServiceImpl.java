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
import com.stockguard.service.ImageStorageService;
import com.stockguard.util.ImageUrlResolver;
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
    private final ImageStorageService imageStorageService;

    @Value("${daryamart.api.base-url}")
    private String baseUrl;

    @Override
    public Optional<BarcodeProductResponseDTO> lookupByBarcode(String barcode) {
        Optional<CatalogProduct> local = catalogProductRepository.findByBarcodeAndIsActiveTrue(barcode);
        if (local.isPresent()) {
            CatalogProduct product = local.get();
            log.info("🏷️ Barcode found in catalog: {}", barcode);

            if (product.getSuggestedSellPrice() == null
                    || !ImageUrlResolver.isStoredKey(product.getImageKey())) {
                enrichFromDaryamart(barcode, product);
            }
            return Optional.of(toResponse(product));
        }

        log.info("🏷️ Barcode not in catalog, falling back to Daryamart: {}", barcode);
        return lookupInDaryamart(barcode);
    }

    /**
     * Catalog hit with a missing price or an image we don't own (null, blank or
     * a legacy external URL saved before MinIO ingestion): ask Daryamart for the
     * missing fields and persist them so the row is complete from here on.
     * Best-effort — the (possibly still incomplete) catalog response is
     * returned either way.
     */
    private void enrichFromDaryamart(String barcode, CatalogProduct product) {
        try {
            log.info("🔄 Catalog product {} lacks price/image, refreshing from Daryamart: {}",
                    product.getId(), barcode);

            findInDaryamart(barcode).ifPresent(dto -> {
                boolean changed = false;

                if (product.getSuggestedSellPrice() == null) {
                    Long rial = toRial(dto.getPrice());
                    if (rial != null) {
                        product.setSuggestedSellPrice(rial);
                        changed = true;
                    }
                }
                if (!ImageUrlResolver.isStoredKey(product.getImageKey())
                        && StringUtils.hasText(dto.getImageAddress())) {
                    String stored = storeImage(dto.getImageAddress());
                    if (stored != null) {
                        product.setImageKey(stored);
                        changed = true;
                    }
                }

                if (changed) {
                    catalogProductRepository.save(product);
                    log.info("✅ Enriched catalog product {} from Daryamart", product.getId());
                } else {
                    log.info("🔍 Daryamart had nothing new for catalog product {}", product.getId());
                }
            });
        } catch (Exception e) {
            log.warn("⚠️ Could not enrich catalog product {} from Daryamart: {}",
                    product.getId(), e.getMessage());
        }
    }

    private Optional<BarcodeProductResponseDTO> lookupInDaryamart(String barcode) {
        return findInDaryamart(barcode).map(product -> {
            CatalogProduct saved = saveToCatalog(barcode, product);
            return saved != null ? toResponse(saved) : toResponse(product);
        });
    }

    private Optional<DaryamartProductDto> findInDaryamart(String barcode) {

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

        return Optional.of(products.get(0));
    }

    /**
     * Stores the Daryamart image in MinIO and returns the object key, so the DB
     * owns the image instead of hotlinking Daryamart. Falls back to the absolute
     * external URL when the download/upload fails — the resolver passes legacy
     * absolute URLs through, so the image still renders.
     */
    private String storeImage(String imageAddress) {
        String absolute = toAbsoluteImageUrl(imageAddress);
        if (absolute == null) {
            return null;
        }
        try {
            String key = imageStorageService.storeFromUrl(absolute);
            log.info("🖼️ Stored Daryamart image in MinIO: {} -> key {}", absolute, key);
            return key;
        } catch (Exception e) {
            log.warn("⚠️ Could not store Daryamart image in MinIO ({}), falling back to direct URL: {}",
                    e.getMessage(), absolute);
            return absolute;
        }
    }

    /**
     * @return the persisted row, or null when the save was skipped/failed —
     * callers then answer from the raw Daryamart payload.
     */
    private CatalogProduct saveToCatalog(String barcode, DaryamartProductDto product) {
        if (product.getId() == null) {
            log.warn("⚠️ Daryamart product has no id, not saving to catalog: {}", barcode);
            return null;
        }

        try {
            Optional<CatalogProduct> existing = catalogProductRepository
                    .findByExternalSourceAndExternalSourceId(EXTERNAL_SOURCE, product.getId());
            if (existing.isPresent()) {
                log.debug("⏭ Daryamart product already in catalog: id={}", product.getId());
                return existing.get();
            }

            CatalogProduct saved = catalogProductRepository.save(CatalogProduct.builder()
                    .name(StringUtils.hasText(product.getName()) ? product.getName() : "بدون نام")
                    .barcode(barcode)
                    .imageKey(storeImage(product.getImageAddress()))
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
            return saved;
        } catch (Exception e) {
            // the lookup response must still be returned even if caching fails
            // (e.g. concurrent save of the same barcode)
            log.warn("⚠️ Could not save Daryamart product to catalog: barcode={}, reason={}",
                    barcode, e.getMessage());
            return null;
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
                .catalogId(product.getId())
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
