package com.stockguard.service.impl;

import com.stockguard.client.DaryamartClient;
import com.stockguard.client.ExternalBarcodeSource;
import com.stockguard.client.ExternalProductDto;
import com.stockguard.client.KowsarplazaSource;
import com.stockguard.client.MartestanSource;
import com.stockguard.client.RojashopSource;
import com.stockguard.client.SehSootehClient;
import com.stockguard.client.SepehrShopSource;
import com.stockguard.client.SoocenterSource;
import com.stockguard.client.TisaMarketClient;
import com.stockguard.data.dto.barcode.response.BarcodeProductResponseDTO;
import com.stockguard.data.dto.barcode.response.EnrichmentBatchStatusDTO;
import com.stockguard.data.entity.CatalogProduct;
import com.stockguard.data.entity.Category;
import com.stockguard.data.entity.Subcategory;
import com.stockguard.repository.CatalogProductRepository;
import com.stockguard.repository.CategoryRepository;
import com.stockguard.repository.SubcategoryRepository;
import com.stockguard.service.BarcodeLookupService;
import com.stockguard.service.ImageStorageService;
import com.stockguard.util.CatalogCompleteness;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Barcode lookup with an ordered chain of external shops: Daryamart first
 * (widest catalog), then the independent sources Kowsarplaza, Rojashop,
 * Martestan (cosmetics/hygiene) and 4soocenter (mweb/WooCommerce), then
 * the remaining Sepehr platform stores (Seh Sooteh, Tisa Market). The
 * first shop that carries the barcode wins; a shop that is unreachable
 * or errors is skipped so the next one still gets a chance.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BarcodeLookupServiceImpl implements BarcodeLookupService {

    private static final String SOURCE_DARYAMART = "DARYAMART";
    private static final String SOURCE_SEHSOOTEH = "SEHSOOTEH";
    private static final String SOURCE_TISAMARKET = "TISAMARKET";
    private static final String UNKNOWN_CATEGORY = "Unknown";

    private final DaryamartClient daryamartClient;
    private final SehSootehClient sehSootehClient;
    private final TisaMarketClient tisaMarketClient;
    private final SoocenterSource soocenterSource;
    private final KowsarplazaSource kowsarplazaSource;
    private final RojashopSource rojashopSource;
    private final MartestanSource martestanSource;
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
                rojashopSource,
                martestanSource,
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

            if (CatalogCompleteness.needsEnrichment(product)) {
                enrichFromShops(barcode, product);
            }
            return Optional.of(toResponse(product));
        }

        log.info("🏷️ Barcode not in catalog, searching external shops: {}", barcode);
        return lookupInShops(barcode);
    }

    /**
     * Catalog hit with a missing title (blank or the "بدون نام" placeholder),
     * sell price, or an image we don't own (null, blank or a legacy external
     * URL saved before MinIO ingestion): ask the external shops for the
     * missing fields and persist them so the row is complete from here on.
     * Shops are tried in order until everything they can supply is filled.
     * Best-effort — the (possibly still incomplete) catalog response is
     * returned either way.
     *
     * @return whether any missing field was actually filled
     */
    private boolean enrichFromShops(String barcode, CatalogProduct product) {
        try {
            log.info("🔄 Catalog product {} lacks title/image/prices, refreshing from external shops: {}",
                    product.getId(), barcode);

            // Record the scan even when no shop contributes anything new:
            // the admin incomplete-products backlog orders by most recently
            // looked-up, and the row is saved unconditionally below.
            product.setLastLookupAt(LocalDateTime.now());

            boolean changed = false;
            for (ExternalBarcodeSource source : shopSources()) {
                boolean titleMissing = CatalogCompleteness.titleMissing(product);
                boolean priceMissing = CatalogCompleteness.sellPriceMissing(product);
                boolean imageMissing = CatalogCompleteness.imageMissing(product);
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

            // always saved: at minimum lastLookupAt is dirty from this attempt
            catalogProductRepository.save(product);
            if (!changed) {
                log.info("🔍 External shops had nothing new for catalog product {}", product.getId());
            }
            return changed;
        } catch (Exception e) {
            log.warn("⚠️ Could not enrich catalog product {} from external shops: {}",
                    product.getId(), e.getMessage());
            return false;
        }
    }

    // ================= BATCH ENRICHMENT =================

    /**
     * Pause between products so a batch run does not hammer seven external
     * shops back-to-back for minutes on end.
     */
    private static final long BATCH_PAUSE_MILLIS = 300;

    private final ExecutorService enrichmentBatchExecutor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "catalog-enrichment-batch");
        t.setDaemon(true);
        return t;
    });

    private volatile EnrichmentBatchState batchState;

    @Override
    public synchronized EnrichmentBatchStatusDTO startEnrichmentBatch() {
        if (batchState != null && batchState.running) {
            log.warn("⏳ Enrichment batch already running: {} of {} done, {} left — not starting another",
                    batchState.processed.get(), batchState.total, batchState.total - batchState.processed.get());
            return batchState.toStatus();
        }

        List<Long> ids = catalogProductRepository.findIncompleteIds(CatalogCompleteness.UNNAMED_PLACEHOLDER);
        batchState = new EnrichmentBatchState(ids.size());
        log.info("🚀 Enrichment batch started: {} incomplete products to try", ids.size());
        enrichmentBatchExecutor.submit(() -> runEnrichmentBatch(ids, batchState));
        return batchState.toStatus();
    }

    @Override
    public EnrichmentBatchStatusDTO getEnrichmentBatchStatus() {
        EnrichmentBatchState state = batchState;
        return state != null ? state.toStatus() : EnrichmentBatchStatusDTO.builder().build();
    }

    private void runEnrichmentBatch(List<Long> ids, EnrichmentBatchState state) {
        try {
            for (Long id : ids) {
                if (Thread.currentThread().isInterrupted()) {
                    log.warn("🛑 Enrichment batch interrupted, stopping early");
                    break;
                }
                try {
                    processBatchProduct(id, state);
                } catch (Exception e) {
                    state.failed.incrementAndGet();
                    log.warn("⚠️ [batch] Unexpected failure on catalog product {}: {}", id, e.getMessage());
                }
                pauseBetweenProducts();
            }
        } finally {
            state.running = false;
            state.finishedAt = LocalDateTime.now();
            log.info("🏁 Enrichment batch finished: processed={} of {}, enriched={}, stillIncomplete={}, failed={}",
                    state.processed.get(), state.total, state.enriched.get(),
                    state.stillIncomplete.get(), state.failed.get());
        }
    }

    private void processBatchProduct(Long id, EnrichmentBatchState state) {
        Optional<CatalogProduct> found = catalogProductRepository.findById(id);
        if (found.isEmpty()) {
            state.processed.incrementAndGet();
            return;
        }
        CatalogProduct product = found.get();
        if (!CatalogCompleteness.needsEnrichment(product)) {
            // completed by a user scan while the batch worked through the list
            log.info("⏭️ [batch {}/{}] product {} already complete, skipping",
                    state.processed.get() + 1, state.total, id);
            state.processed.incrementAndGet();
            return;
        }

        state.currentBarcode = product.getBarcode();
        log.info("🔎 [batch {}/{}] looking up product {} (barcode={}, name='{}') in external shops",
                state.processed.get() + 1, state.total, id, product.getBarcode(), product.getName());

        boolean changed = enrichFromShops(product.getBarcode(), product);
        boolean complete = !CatalogCompleteness.needsEnrichment(product);

        if (changed) {
            state.enriched.incrementAndGet();
        }
        if (!complete) {
            state.stillIncomplete.incrementAndGet();
        }
        state.processed.incrementAndGet();

        String outcome = complete
                ? "complete now"
                : changed
                        ? "partially enriched, still missing " + missingFields(product)
                        : "shops had nothing new, still missing " + missingFields(product);
        log.info("{} [batch] product {} (barcode={}): {} ({} left)",
                complete ? "✅" : changed ? "🟡" : "❌", id, product.getBarcode(), outcome,
                state.total - state.processed.get());
    }

    private String missingFields(CatalogProduct product) {
        List<String> missing = new ArrayList<>();
        if (CatalogCompleteness.titleMissing(product)) {
            missing.add("title");
        }
        if (CatalogCompleteness.sellPriceMissing(product)) {
            missing.add("price");
        }
        if (CatalogCompleteness.imageMissing(product)) {
            missing.add("image");
        }
        return String.join(", ", missing);
    }

    private void pauseBetweenProducts() {
        try {
            Thread.sleep(BATCH_PAUSE_MILLIS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @PreDestroy
    void shutdownEnrichmentBatch() {
        enrichmentBatchExecutor.shutdownNow();
    }

    /**
     * Mutable progress of the running/last batch; written only by the
     * single batch thread, read by status requests.
     */
    private static final class EnrichmentBatchState {
        final int total;
        final LocalDateTime startedAt = LocalDateTime.now();
        final AtomicInteger processed = new AtomicInteger();
        final AtomicInteger enriched = new AtomicInteger();
        final AtomicInteger stillIncomplete = new AtomicInteger();
        final AtomicInteger failed = new AtomicInteger();
        volatile boolean running = true;
        volatile LocalDateTime finishedAt;
        volatile String currentBarcode;

        EnrichmentBatchState(int total) {
            this.total = total;
        }

        EnrichmentBatchStatusDTO toStatus() {
            return EnrichmentBatchStatusDTO.builder()
                    .running(running)
                    .currentBarcode(currentBarcode)
                    .total(total)
                    .processed(processed.get())
                    .remaining(Math.max(0, total - processed.get()))
                    .enriched(enriched.get())
                    .stillIncomplete(stillIncomplete.get())
                    .failed(failed.get())
                    .startedAt(startedAt)
                    .finishedAt(finishedAt)
                    .build();
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
                    .name(StringUtils.hasText(product.name()) ? product.name() : CatalogCompleteness.UNNAMED_PLACEHOLDER)
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
                    .lastLookupAt(LocalDateTime.now())
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
