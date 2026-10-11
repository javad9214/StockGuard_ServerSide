package com.stockguard.client;

import com.stockguard.data.dto.sepehr.SepehrProductDto;
import com.stockguard.data.dto.sepehr.SepehrSearchResponseDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Optional;

/**
 * One shop on the Sepehr platform (فروشگاه ساز سپهر): Daryamart, Seh Sooteh,
 * Tisa Market. Searching by the product's barcode as {@code Key} is an exact
 * match; prices are Toman (×10 → Rial) and image addresses are relative to
 * the shop's own domain.
 */
@Slf4j
public class SepehrShopSource implements ExternalBarcodeSource {

    private static final int PAGE_NUMBER = 1;
    private static final int PAGE_SIZE = 50;

    private final String name;
    private final String baseUrl;
    private final SepehrShopApi client;

    public SepehrShopSource(String name, String baseUrl, SepehrShopApi client) {
        this.name = name;
        this.baseUrl = baseUrl;
        this.client = client;
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public Optional<ExternalProductDto> findByBarcode(String barcode) {
        SepehrSearchResponseDto response = client.searchProducts(barcode, PAGE_NUMBER, PAGE_SIZE);

        if (response == null || !Boolean.TRUE.equals(response.getSuccess())
                || response.getData() == null) {
            log.warn("⚠️ {} search failed for barcode {}: {}",
                    name, barcode, response != null ? response.getMessage() : "null response");
            return Optional.empty();
        }

        List<SepehrProductDto> products = response.getData().getSource();
        if (products == null || products.isEmpty()) {
            log.info("🔍 No {} product found for barcode: {}", name, barcode);
            return Optional.empty();
        }

        SepehrProductDto product = products.get(0);
        return Optional.of(new ExternalProductDto(
                product.getName(),
                toRial(product.getPrice()),
                toAbsoluteImageUrl(product.getImageAddress()),
                product.getId()));
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
