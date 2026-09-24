package com.stockguard.service;

import com.stockguard.data.dto.barcode.response.BarcodeProductResponseDTO;

import java.util.Optional;

public interface BarcodeLookupService {

    /**
     * Looks up a product by barcode in the Daryamart catalog.
     *
     * @param barcode product barcode
     * @return the first matching product, or empty when nothing matches
     */
    Optional<BarcodeProductResponseDTO> lookupByBarcode(String barcode);
}
