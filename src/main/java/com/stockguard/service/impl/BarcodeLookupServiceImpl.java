package com.stockguard.service.impl;

import com.stockguard.client.DaryamartClient;
import com.stockguard.client.ExternalBarcodeSource;
import com.stockguard.client.ExternalProductDto;
import com.stockguard.client.KowsarplazaSource;
import com.stockguard.client.SehSootehClient;
import com.stockguard.client.SepehrShopSource;
import com.stockguard.client.SoocenterSource;
import com.stockguard.client.TisaMarketClient;
import com.stockguard.data.dto.barcode.response.BarcodeProductResponseDTO;
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

import java.util.List;
import java.util.Optional;

/**
 * Barcode lookup with an ordered chain of external shops: Daryamart first
 * (widest catalog), then the mweb/WooCommerce sources Kowsarplaza and
 * 4soocenter, then the remaining Sepehr platform stores (Seh Sooteh, Tisa
 * Market). The first shop that carries the barcode wins; a shop that is
 * unreachable or errors is skipped so the next one still gets a chance.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BarcodeLookupServiceImpl implements BarcodeLookupService {

    private static final String SOURCE_DARYAMART = "DARYAMART";
    private static final String SOURCE_SEHSOOTEH = "SEHSOOTEH";
    private static final String SOURCE_TISAMARKET = "TISAMARKET";
    private static final String UNKNOWN_CATEGORY = "Unknown";
    private static final String UNNAMED_PLACEHOLDER = "بدون نام";

    private final DaryamartClient daryamartClient;
    private final SehSootehClient sehSootehClient;
    private final TisaMarketClient tisaMarketClient;
    private final SoocenterSource soocenterSource;
    private final KowsarplazaSource kowsarplazaSource;
    private final CatalogProductRepository catalogProductRepository;
    private final CategoryRepository categoryRepository;
    private final SubcategoryRepository subcategoryRepository;
    private final ImageStorageService imageStorageService;

    @Value("${daryamart.api.base-url}")
    private String daryamartBaseUrl;

    @Value("${sehsooteh.api.base-url}")
    private String sehSootehBaseUrl;

    @Value("${tisamarket.api.base-url}")
    private String tisamarketBaseUrl;

    private List<ExternalBarcodeSource> shopSources() {
        return List.of(
                new SepehrShopSource(SOURCE_DARYAMART, daryamartBaseUrl, daryamartClient),
                kowsarplazaSource,
                soocenterSource,
                new SepehrShopSource(SOURCE_SEHSOOTEH, sehSootehBaseUrl, sehSootehClient),
                new SepehrShopSource(SOURCE_TISAMARKET, tisamarketBaseUrl, tisaMarketClient));
    }

    @Override
    public Optional<BarcodeProductResponseDTO> lookupByBarcode(String barcode) {
        Optional<CatalogProduct> local = catalogProductRepository.findByBarcodeAndIsActiveTrue(barcode);
        if (local.isPresent()) {
            CatalogProduct product = local.get();
            log.info("🏷️ Barcode found in catalog: {}", barcode);

            if (needsEnrichment(product)) {
                enrichFromShops(barcode, product);
            }
            return Optional.of(toResponse(product));
        }

        log.info("🏷️ Barcode not in catalog, searching external shops: {}", barcode);
        return lookupInShops(barcode);
    }

    /**
     * A catalog hit short-circuits (no external calls) only when the row is
     * complete: a real title, a stored image and a sell price. Cost price is
     * deliberately not part of the trigger — the shops never return one, so
     * including it would re-query them on every lookup with nothing to
     * gain; cost price is only filled by admin/user data entry.
     */
    private boolean needsEnrichment(CatalogProduct product) {
        return titleMissing(product)
                || product.getSuggestedSellPrice() == null
                || !ImageUrlResolver.isStoredKey(product.getImageKey());
    }

    private boolean titleMissing(CatalogProduct product) {
        return !StringUtils.hasText(product.getName())
                || UNNAMED_PLACEHOLDER.equals(product.getName());
    }

    /**
     * Catalog hit with a missing title (blank or the "بدون نام" placeholder),
     * sell price, or an image we don't own (null, blank or a legacy external
     * URL saved before MinIO ingestion): ask the external shops for the
     * missing fields and persist them so the row is complete from here on.
     * Shops are tried in order until everything they can supply is filled.
     * Best-effort — the (possibly still incomplete) catalog response is
     * returned either way.
     */
    private void enrichFromShops(String barcode, CatalogProduct product) {
        try {
            log.info("🔄 Catalog product {} lacks title/image/prices, refreshing from external shops: {}",
                    product.getId(), barcode);

            boolean changed = false;
            for (ExternalBarcodeSource source : shopSources()) {
                boolean titleMissing = titleMissing(product);
                boolean priceMissing = product.getSuggestedSellPrice() == null;
                boolean imageMissing = !ImageUrlResolver.isStoredKey(product.getImageKey());
                if (!titleMissing && !priceMissing && !imageMissing) {
                    break;
                }

                Optional<ExternalProductDto> found = findInShop(source, barcode);
                if (found.isEmpty()) {
                    continue;
                }
                ExternalProductDto dto = found.get();
                boolean contributed = false;

                if (titleMissing && StringUtils.hasText(dto.name())) {
                    product.setName(dto.name());
                    contributed = true;
                }
                if (priceMissing) {
                    Long rial = dto.priceRial();
                    if (rial != null) {
                        product.setSuggestedSellPrice(rial);
                        contributed = true;
                    }
                }
                if (imageMissing && StringUtils.hasText(dto.imageUrl())) {
                    String stored = storeImage(source.name(), dto.imageUrl());
                    if (stored != null) {
                        product.setImageKey(stored);
                        contributed = true;
                    }
                }

                if (contributed) {
                    changed = true;
                    log.info("✅ Enriched catalog product {} from {}",
                            product.getId(), source.name());
                }
            }

            if (changed) {
                catalogProductRepository.save(product);
            } else {
                log.info("🔍 External shops had nothing new for catalog product {}", product.getId());
            }
        } catch (Exception e) {
            log.warn("⚠️ Could not enrich catalog product {} from external shops: {}",
                    product.getId(), e.getMessage());
        }
    }

    private Optional<BarcodeProductResponseDTO> lookupInShops(String barcode) {
        for (ExternalBarcodeSource source : shopSources()) {
            Optional<ExternalProductDto> found = findInShop(source, barcode);
            if (found.isPresent()) {
                CatalogProduct saved = saveToCatalog(source, barcode, found.get());
                return Optional.of(saved != null ? toResponse(saved) : toResponse(found.get()));
            }
        }
        log.info("🔍 No external shop carries barcode: {}", barcode);
        return Optional.empty();
    }

    /**
     * Queries one shop; a transport failure (timeout, shop down) only skips
     * that shop so the chain can continue with the next one.
     */
    private Optional<ExternalProductDto> findInShop(ExternalBarcodeSource source, String barcode) {
        try {
            return source.findByBarcode(barcode);
        } catch (Exception e) {
            log.warn("⚠️ {} lookup failed for barcode {}: {}", source.name(), barcode, e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Stores the shop image in MinIO and returns the object key, so the DB
     * owns the image instead of hotlinking the shop. Falls back to the absolute
     * external URL when the download/upload fails — the resolver passes legacy
     * absolute URLs through, so the image still renders.
     */
    private String storeImage(String sourceName, String absoluteImageUrl) {
        if (absoluteImageUrl == null) {
            return null;
        }
        try {
            String key = imageStorageService.storeFromUrl(absoluteImageUrl);
            log.info("🖼️ Stored {} image in MinIO: {} -> key {}", sourceName, absoluteImageUrl, key);
            return key;
        } catch (Exception e) {
            // S3Exception#toString is a bare "Bad Request" — surface MinIO's
            // own error code/body so a persistent failure is diagnosable
            String detail = e instanceof software.amazon.awssdk.services.s3.model.S3Exception s3
                    ? "status=" + s3.statusCode()
                    + ", requestId=" + s3.requestId()
                    + ", code=" + (s3.awsErrorDetails() != null ? s3.awsErrorDetails().errorCode() : null)
                    + ", message=" + (s3.awsErrorDetails() != null ? s3.awsErrorDetails().errorMessage() : null)
                    : e.toString();
            log.warn("⚠️ Could not store {} image in MinIO ({}), falling back to direct URL: {}",
                    sourceName, detail, absoluteImageUrl, e);
            return absoluteImageUrl;
        }
    }

    /**
     * @return the persisted row, or null when the save was skipped/failed —
     * callers then answer from the raw shop payload.
     */
    private CatalogProduct saveToCatalog(ExternalBarcodeSource source, String barcode, ExternalProductDto product) {
        if (product.externalId() == null) {
            log.warn("⚠️ {} product has no id, not saving to catalog: {}", source.name(), barcode);
            return null;
        }

        try {
            Optional<CatalogProduct> existing = catalogProductRepository
                    .findByExternalSourceAndExternalSourceId(source.name(), product.externalId());
            if (existing.isPresent()) {
                log.debug("⏭ {} product already in catalog: id={}", source.name(), product.externalId());
                return existing.get();
            }

            CatalogProduct saved = catalogProductRepository.save(CatalogProduct.builder()
                    .name(StringUtils.hasText(product.name()) ? product.name() : UNNAMED_PLACEHOLDER)
                    .barcode(barcode)
                    .imageKey(storeImage(source.name(), product.imageUrl()))
                    .suggestedSellPrice(product.priceRial())
                    .externalSource(source.name())
                    .externalSourceId(product.externalId())
                    .imageSource(source.name())
                    .subcategory(resolveUnknownSubcategory())
                    .status(CatalogProduct.CatalogStatus.VERIFIED)
                    .qualityScore(70)
                    .adoptionCount(0)
                    .isActive(true)
                    .build());

            log.info("✅ Saved {} product to catalog: barcode={}, name={}, catalogId={}",
                    source.name(), barcode, saved.getName(), saved.getId());
            return saved;
        } catch (Exception e) {
            // the lookup response must still be returned even if caching fails
            // (e.g. concurrent save of the same barcode)
            log.warn("⚠️ Could not save {} product to catalog: barcode={}, reason={}",
                    source.name(), barcode, e.getMessage());
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

    private BarcodeProductResponseDTO toResponse(ExternalProductDto product) {
        return BarcodeProductResponseDTO.builder()
                .name(product.name())
                .imageUrl(product.imageUrl())
                .sellPrice(product.priceRial())
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
}
