package com.stockguard.controller;

import com.stockguard.data.dto.barcode.request.BarcodeLookupRequestDTO;
import com.stockguard.data.dto.barcode.response.BarcodeProductResponseDTO;
import com.stockguard.data.dto.common.ResponseDTO;
import com.stockguard.data.enums.ResponseCode;
import com.stockguard.service.BarcodeLookupService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/barcode")
@RequiredArgsConstructor
public class BarcodeLookupController extends BaseController {

    static final String BARCODE_NOT_FOUND_MESSAGE = "محصولی با این بارکد یافت نشد";

    private final BarcodeLookupService barcodeLookupService;

    /**
     * Look up a product by barcode
     * POST /api/barcode/lookup
     */
    @PostMapping("/lookup")
    public ResponseEntity<ResponseDTO<BarcodeProductResponseDTO>> lookupByBarcode(
            @Valid @RequestBody BarcodeLookupRequestDTO request) {
        try {
            return barcodeLookupService.lookupByBarcode(request.getBarcode())
                    .map(product -> {
                        log.info("✅ product found: {}", product.getName());
                        return generateOKResponse(product);
                    })
                    .orElseGet(() -> {
                        log.warn("⚠️ No product for barcode: {}", request.getBarcode());
                        return generateErrorResponse(HttpStatus.NOT_FOUND, ResponseCode.NOT_FOUND, BARCODE_NOT_FOUND_MESSAGE);
                    });
        } catch (Exception e) {
            log.error("❌ barcode lookup failed for {}: {}", request.getBarcode(), e.getMessage(), e);
            return generateErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, ResponseCode.INTERNAL_ERROR, null);
        }
    }
}
