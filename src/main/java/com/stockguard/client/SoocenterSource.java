package com.stockguard.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import java.net.URI;
import java.time.Duration;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * فروشگاه چارسو (4soocenter.ir) — a WooCommerce shop on the popular Persian
 * "mweb" theme. Its live search matches the product SKU, and the shop keeps
 * real barcodes in the SKU field, so the theme's admin-ajax endpoint gives
 * an exact barcode lookup. The answer is an HTML snippet; the linked product
 * page carries JSON-LD (Product) whose SKU re-verifies the match — a result
 * whose SKU does not equal the queried barcode is rejected, so fuzzy hits
 * can never reach the catalog. Prices are Rial.
 *
 * Any other mweb-theme shop can become a source by copying this class and
 * pointing it at the new base URL.
 */
@Slf4j
@Component
public class SoocenterSource implements ExternalBarcodeSource {

    public static final String SOURCE_NAME = "SOOCENTER";

    private static final String SEARCH_ACTION = "mweb_theme_ajax_search";
    private static final String NOT_FOUND_MARKER = "header-search-not-found";
    private static final Pattern RESULT_LINK = Pattern.compile("<a\\s+href=\"([^\"]+)\"\\s+title=\"([^\"]*)\"");
    private static final Pattern TRAILING_ID = Pattern.compile("-(\\d+)/?(?:\\?|$)");
    private static final Pattern SKU = Pattern.compile("\"sku\"\\s*:\\s*\"([^\"]+)\"");
    private static final Pattern JSONLD_NAME = Pattern.compile("\"name\"\\s*:\\s*\"([^\"]+)\"");
    private static final Pattern JSONLD_PRICE = Pattern.compile("\"price\"\\s*:\\s*\"(\\d+(?:\\.\\d+)?)\"");
    private static final Pattern JSONLD_CURRENCY = Pattern.compile("\"priceCurrency\"\\s*:\\s*\"([A-Za-z]+)\"");
    private static final Pattern JSONLD_IMAGE = Pattern.compile("\"image\"\\s*:\\s*(?:\"([^\"]+)\"|\\[\\s*\"([^\"]+)\")");
    private static final Pattern SNIPPET_PRICE = Pattern.compile("<bdi>\\s*([\\d,]+)");

    private final RestClient httpClient = RestClient.builder()
            .requestFactory(requestFactory())
            .defaultHeader(HttpHeaders.USER_AGENT, "Mozilla/5.0 (compatible; StockGuard-lookup)")
            .build();

    @Value("${soocenter.api.base-url}")
    private String baseUrl;

    @Override
    public String name() {
        return SOURCE_NAME;
    }

    @Override
    public Optional<ExternalProductDto> findByBarcode(String barcode) {
        String snippet = searchSnippet(barcode);
        if (!StringUtils.hasText(snippet) || snippet.contains(NOT_FOUND_MARKER)) {
            log.info("🔍 No {} product found for barcode: {}", SOURCE_NAME, barcode);
            return Optional.empty();
        }

        Matcher link = RESULT_LINK.matcher(snippet);
        if (!link.find()) {
            log.warn("⚠️ {} returned an unrecognized snippet for barcode {}", SOURCE_NAME, barcode);
            return Optional.empty();
        }
        String productUrl = link.group(1);
        String snippetTitle = link.group(2).trim();

        // re-verify on the product page: its JSON-LD SKU must equal the barcode
        String page = fetchProductPage(productUrl);
        String name = StringUtils.hasText(snippetTitle) ? snippetTitle : null;
        Long price = null;
        String image = null;
        if (page != null) {
            int skuAt = indexOfSku(page, barcode);
            if (skuAt < 0) {
                log.warn("⚠️ {} product {} does not carry barcode {} in its SKU — rejecting hit",
                        SOURCE_NAME, productUrl, barcode);
                return Optional.empty();
            }
            String block = productBlockAround(page, skuAt);
            Matcher m = JSONLD_NAME.matcher(block);
            if (m.find()) {
                name = m.group(1);
            }
            m = JSONLD_CURRENCY.matcher(block);
            String currency = m.find() ? m.group(1).toUpperCase() : "IRR";
            m = JSONLD_PRICE.matcher(block);
            if (m.find()) {
                price = normalizePrice(m.group(1), currency);
            }
            m = JSONLD_IMAGE.matcher(block);
            if (m.find()) {
                image = m.group(1) != null ? m.group(1) : m.group(2);
            }
        }
        if (price == null) {
            // fall back to the search snippet amounts (Rial, sale price first)
            Matcher p = SNIPPET_PRICE.matcher(snippet);
            Long last = null;
            while (p.find()) {
                last = Long.parseLong(p.group(1).replace(",", ""));
            }
            price = last;
        }

        return Optional.of(new ExternalProductDto(name, price, image, trailingId(productUrl)));
    }

    /** package-private seams so unit tests can stub the HTTP calls. */
    protected String searchSnippet(String barcode) {
        return httpClient.post()
                .uri(baseUrl + "/wp-admin/admin-ajax.php")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                // barcode is digits-only, no form-encoding concerns
                .body("action=" + SEARCH_ACTION + "&cat_id=&s=" + barcode)
                .retrieve()
                .body(String.class);
    }

    protected String fetchProductPage(String productUrl) {
        return httpClient.get()
                .uri(URI.create(productUrl))
                .retrieve()
                .body(String.class);
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

    /** the JSON-LD Product block around the SKU match (Breadcrumbs excluded) */
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

    /** محصول-4928/ style slugs carry the shop's numeric product id. */
    private Long trailingId(String productUrl) {
        Matcher m = TRAILING_ID.matcher(productUrl);
        return m.find() ? Long.parseLong(m.group(1)) : null;
    }

    private static SimpleClientHttpRequestFactory requestFactory() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(5));
        factory.setReadTimeout(Duration.ofSeconds(10));
        return factory;
    }
}
