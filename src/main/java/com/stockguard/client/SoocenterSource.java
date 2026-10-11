package com.stockguard.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * فروشگاه چارسو (4soocenter.ir) — mweb-theme shop whose product pages carry
 * JSON-LD (Product) with the SKU; the SKU must equal the queried barcode, so
 * fuzzy hits can never reach the catalog. Prices are Rial (Toman labels ×10).
 */
@Slf4j
@Component
public class SoocenterSource extends MwebShopSource {

    public static final String SOURCE_NAME = "SOOCENTER";

    private static final Pattern SKU = Pattern.compile("\"sku\"\\s*:\\s*\"([^\"]+)\"");
    private static final Pattern JSONLD_NAME = Pattern.compile("\"name\"\\s*:\\s*\"([^\"]+)\"");
    private static final Pattern JSONLD_PRICE = Pattern.compile("\"price\"\\s*:\\s*\"(\\d+(?:\\.\\d+)?)\"");
    private static final Pattern JSONLD_CURRENCY = Pattern.compile("\"priceCurrency\"\\s*:\\s*\"([A-Za-z]+)\"");
    private static final Pattern JSONLD_IMAGE = Pattern.compile("\"image\"\\s*:\\s*(?:\"([^\"]+)\"|\\[\\s*\"([^\"]+)\")");

    public SoocenterSource(@Value("${soocenter.api.base-url}") String baseUrl) {
        super(SOURCE_NAME, baseUrl);
    }

    @Override
    protected Optional<ExternalProductDto> parseHit(
            String snippetTitle, String productUrl, String snippet, String page, String barcode) {
        if (page == null) {
            // SKU-keyed search already matched; the snippet is the best we have
            return Optional.of(new ExternalProductDto(
                    snippetTitle, lastSnippetAmount(snippet), null, trailingSlugId(productUrl)));
        }

        // re-verify on the product page: its JSON-LD SKU must equal the barcode
        int skuAt = indexOfSku(page, barcode);
        if (skuAt < 0) {
            log.warn("⚠️ {} product {} does not carry barcode {} in its SKU — rejecting hit",
                    SOURCE_NAME, productUrl, barcode);
            return Optional.empty();
        }

        String block = productBlockAround(page, skuAt);
        String name = snippetTitle;
        Matcher m = JSONLD_NAME.matcher(block);
        if (m.find()) {
            name = m.group(1);
        }
        Long price = null;
        m = JSONLD_CURRENCY.matcher(block);
        String currency = m.find() ? m.group(1).toUpperCase() : "IRR";
        m = JSONLD_PRICE.matcher(block);
        if (m.find()) {
            price = normalizePrice(m.group(1), currency);
        }
        String image = null;
        m = JSONLD_IMAGE.matcher(block);
        if (m.find()) {
            image = m.group(1) != null ? m.group(1) : m.group(2);
        }

        if (price == null) {
            // fall back to the search snippet amounts (Rial)
            price = lastSnippetAmount(snippet);
        }
        return Optional.of(new ExternalProductDto(name, price, image, trailingSlugId(productUrl)));
    }

    private int indexOfSku(String page, String barcode) {
        Matcher m = SKU.matcher(page);
        while (m.find()) {
            if (barcode.equals(m.group(1))) {
                return m.start();
            }
        }
        return -1;
    }

    /** the JSON-LD Product block around the SKU match (Breadcrumbs excluded). */
    private String productBlockAround(String page, int skuAt) {
        int typeAt = page.lastIndexOf("\"@type\":\"Product\"", skuAt);
        int from = typeAt >= 0 ? typeAt : Math.max(0, skuAt - 500);
        return page.substring(from, Math.min(page.length(), skuAt + 1200));
    }

    private Long normalizePrice(String raw, String currency) {
        long value = (long) Double.parseDouble(raw);
        if (value <= 0) {
            return null;
        }
        // Woo reports IRR (Rial); treat Toman labels as needing the ×10
        return "TMN".equals(currency) || "IRT".equals(currency) ? value * 10 : value;
    }
}
