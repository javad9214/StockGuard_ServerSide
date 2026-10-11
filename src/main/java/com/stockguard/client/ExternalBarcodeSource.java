package com.stockguard.client;

import java.util.Optional;

/**
 * One external shop the barcode-lookup chain can ask. Implementations are
 * responsible for their own protocol (Sepehr JSON API, mweb admin-ajax
 * HTML, …) and normalize the answer to {@link ExternalProductDto}. Prices
 * are always Rial; images are absolute URLs (or null when the shop has
 * none). Transport failures surface as exceptions — the chain skips the
 * source and tries the next one.
 */
public interface ExternalBarcodeSource {

    /** Stable source name persisted in external_source / image_source. */
    String name();

    /**
     * @return the shop's product for this exact barcode, or empty when the
     *         shop does not carry it.
     */
    Optional<ExternalProductDto> findByBarcode(String barcode);
}
