package com.stockguard.client;

/**
 * Normalized product row returned by any {@link ExternalBarcodeSource}:
 * prices in Rial, images as absolute URLs; every field except the name may
 * be null when the shop does not provide it.
 */
public record ExternalProductDto(
        String name,
        Long priceRial,
        String imageUrl,
        Long externalId) {
}
