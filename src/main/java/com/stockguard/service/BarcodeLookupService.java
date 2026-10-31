package com.stockguard.service;

import com.stockguard.data.dto.barcode.response.BarcodeProductResponseDTO;
import com.stockguard.data.dto.barcode.response.EnrichmentBatchStatusDTO;

import java.util.Optional;

public interface BarcodeLookupService {

    /**
     * Looks up a product by barcode in the Daryamart catalog.
     *
     * @param barcode product barcode
     * @return the first matching product, or empty when nothing matches
     */
    Optional<BarcodeProductResponseDTO> lookupByBarcode(String barcode);

    /**
     * Starts (or reports, when one is already running) a background batch
     * that re-runs shop enrichment for every catalog product still missing
     * a title, sell price or stored image. Returns immediately with the
     * current progress; detail goes to the server logs.
     */
    EnrichmentBatchStatusDTO startEnrichmentBatch();

    /**
     * @return progress of the running or last enrichment batch; zeroed
     * when no batch was ever started
     */
    EnrichmentBatchStatusDTO getEnrichmentBatchStatus();
}
