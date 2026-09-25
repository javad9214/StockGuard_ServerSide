package com.stockguard.service.impl;

import com.stockguard.client.DaryamartClient;
import com.stockguard.data.dto.barcode.response.BarcodeProductResponseDTO;
import com.stockguard.data.dto.daryamart.DaryamartProductDto;
import com.stockguard.data.dto.daryamart.DaryamartSearchResponseDto;
import com.stockguard.data.entity.CatalogProduct;
import com.stockguard.repository.CatalogProductRepository;
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

    private static final BigDecimal TOMAN_TO_RIAL = BigDecimal.TEN;

    private final DaryamartClient daryamartClient;
    private final CatalogProductRepository catalogProductRepository;

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

        return Optional.of(toResponse(products.get(0)));
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
