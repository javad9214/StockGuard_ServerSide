package com.stockguard.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import java.net.URI;
import java.time.Duration;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Base for the Persian "mweb" (digiland-pro) WooCommerce theme family. The
 * theme's admin-ajax live search is keyed on the product SKU and these shops
 * keep real barcodes there, so a form-encoded POST of the barcode returns an
 * HTML snippet whose first result link points at the product page.
 *
 * Subclasses turn one such hit into an {@link ExternalProductDto} — and are
 * responsible for verifying that the hit really carries the queried barcode
 * (JSON-LD SKU, title convention, …): the search box echoing a barcode back
 * is not by itself proof, so {@link #parseHit} must return empty on any
 * doubt. Prices are normalized to Rial.
 */
@Slf4j
public abstract class MwebShopSource implements ExternalBarcodeSource {

    protected static final String SEARCH_ACTION = "mweb_theme_ajax_search";
    protected static final String NOT_FOUND_MARKER = "header-search-not-found";

    protected static final Pattern RESULT_LINK = Pattern.compile("<a\\s+href=\"([^\"]+)\"\\s+title=\"([^\"]*)\"");
    protected static final Pattern BDI_AMOUNT = Pattern.compile("<bdi>\\s*([\\d.,]+)");
    protected static final Pattern SNIPPET_IMAGE = Pattern.compile("<img[^>]+src=\"([^\"]+)\"");
    protected static final Pattern TRAILING_ID = Pattern.compile("-(\\d+)/?(?:\\?|$)");

    private final String sourceName;
    private final String baseUrl;

    private final RestClient httpClient = RestClient.builder()
            .requestFactory(requestFactory())
            .defaultHeader(HttpHeaders.USER_AGENT, "Mozilla/5.0 (compatible; StockGuard-lookup)")
            .build();

    protected MwebShopSource(String sourceName, String baseUrl) {
        this.sourceName = sourceName;
        this.baseUrl = baseUrl;
    }

    @Override
    public final String name() {
        return sourceName;
    }

    @Override
    public final Optional<ExternalProductDto> findByBarcode(String barcode) {
        String snippet = searchSnippet(barcode);
        if (!StringUtils.hasText(snippet) || snippet.contains(NOT_FOUND_MARKER)) {
            log.info("🔍 No {} product found for barcode: {}", sourceName, barcode);
            return Optional.empty();
        }

        Matcher link = RESULT_LINK.matcher(snippet);
        if (!link.find()) {
            log.warn("⚠️ {} returned an unrecognized snippet for barcode {}", sourceName, barcode);
            return Optional.empty();
        }

        String productUrl = link.group(1);
        String page = fetchProductPage(productUrl);
        return parseHit(link.group(2).trim(), productUrl, snippet, page, barcode);
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

    /**
     * Maps one search hit to a catalog-ready product, or empty to reject it.
     * Implementations MUST verify the hit carries the exact queried barcode
     * in whatever way the shop exposes the SKU before answering.
     */
    protected abstract Optional<ExternalProductDto> parseHit(
            String snippetTitle, String productUrl, String snippet, String page, String barcode);

    /** last &lt;bdi&gt; amount in the snippet — Woo renders the sale price last. */
    protected Long lastSnippetAmount(String snippet) {
        Matcher p = BDI_AMOUNT.matcher(snippet);
        Long last = null;
        while (p.find()) {
            last = parseAmount(p.group(1));
        }
        return last;
    }

    /** digits with ',' or '.' thousand separators — fa-IR shops use the dot. */
    protected Long parseAmount(String raw) {
        try {
            long value = Long.parseLong(raw.replace(".", "").replace(",", ""));
            return value > 0 ? value : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /** the search snippet thumbnail, when present. */
    protected String snippetImage(String snippet) {
        Matcher m = SNIPPET_IMAGE.matcher(snippet);
        return m.find() ? m.group(1) : null;
    }

    /** محصول-4928/ style slugs carry the shop's numeric product id. */
    protected Long trailingSlugId(String productUrl) {
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
