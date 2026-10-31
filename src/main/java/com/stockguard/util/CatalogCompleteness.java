package com.stockguard.util;

import com.stockguard.data.entity.CatalogProduct;
import org.springframework.util.StringUtils;

/**
 * Single source of truth for "which catalog fields count as missing" —
 * shared by the barcode-lookup enrichment trigger and the admin
 * incomplete-products backlog, so the two can never drift apart.
 *
 * The SQL twin of these predicates lives in
 * {@code CatalogProductRepository#findIncomplete} — keep both in sync.
 */
public final class CatalogCompleteness {

    /** Title placeholder written when an external shop returned no name. */
    public static final String UNNAMED_PLACEHOLDER = "بدون نام";

    private CatalogCompleteness() {
    }

    /**
     * A catalog hit short-circuits (no external calls) only when the row is
     * complete: a real title, a stored image and a sell price. Cost price is
     * deliberately not part of the trigger — the shops never return one, so
     * including it would re-query them on every lookup with nothing to
     * gain; cost price is only filled by admin/user data entry.
     */
    public static boolean needsEnrichment(CatalogProduct product) {
        return titleMissing(product)
                || sellPriceMissing(product)
                || imageMissing(product);
    }

    public static boolean titleMissing(CatalogProduct product) {
        return !StringUtils.hasText(product.getName())
                || UNNAMED_PLACEHOLDER.equals(product.getName());
    }

    public static boolean sellPriceMissing(CatalogProduct product) {
        return product.getSuggestedSellPrice() == null;
    }

    /**
     * @return true when the image is not one we own — null, blank, or a
     * legacy external absolute URL saved before MinIO ingestion
     */
    public static boolean imageMissing(CatalogProduct product) {
        return !ImageUrlResolver.isStoredKey(product.getImageKey());
    }
}
