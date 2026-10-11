package com.stockguard.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
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
 * فروشگاه اینترنتی مارتستان (martestan.com) — custom PHP shop whose live
 * search (POST /search with the params in the QUERY string —
 * title=&lt;barcode&gt;&amp;catid=0&amp;catseo=&amp;ajax=ok; the form body is ignored)
 * is fuzzy: a barcode can return a sibling variant, so the snippet alone
 * proves nothing. Every product page renders the shop's own product code
 * in a jQuery snippet (کد محصول : &lt;span&gt;…&lt;/span&gt;) and the shop keeps
 * real barcodes there — a hit only counts when that code equals the
 * queried barcode, and no page means no hit. The meta
 * product_price is Toman (×10 → Rial; 0 when out of stock), mpn carries
 * the numeric product id and og:image the full-size photo.
 */
@Slf4j
@Component
public class MartestanSource implements ExternalBarcodeSource {

    public static final String SOURCE_NAME = "MARTESTAN";

    private static final Pattern ERROR_MARKER = Pattern.compile("<div class=\"error\">");
    private static final Pattern RESULT_LINK = Pattern.compile("<a href=\"([^\"]+)\" class=\"title\">([^<]*)");
    private static final Pattern SNIPPET_IMAGE = Pattern.compile("<img[^>]+src=\"([^\"]+)\"");
    private static final Pattern PRODUCT_CODE = Pattern.compile("کد محصول\\s*:\\s*<span>(\\d+)</span>");
    private static final Pattern PRICE_META = Pattern.compile("<meta name=\"product_price\" content=\"(\\d+)\"");
    private static final Pattern MPN = Pattern.compile("\"mpn\"\\s*:\\s*\"(\\d+)\"");
    private static final Pattern OG_IMAGE = Pattern.compile("<meta property=\"og:image\" content=\"([^\"]+)\"");

    private final String baseUrl;

    private final RestClient httpClient = RestClient.builder()
            .requestFactory(requestFactory())
            .defaultHeader(HttpHeaders.USER_AGENT,
                    "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 "
                            + "(KHTML, like Gecko) Chrome/126.0 Safari/537.36")
            .defaultHeader(HttpHeaders.ACCEPT_LANGUAGE, "fa-IR,fa;q=0.9,en;q=0.8")
            .build();

    public MartestanSource(@Value("${martestan.api.base-url}") String baseUrl) {
        this.baseUrl = baseUrl;
    }

    @Override
    public String name() {
        return SOURCE_NAME;
    }

    @Override
    public Optional<ExternalProductDto> findByBarcode(String barcode) {
        String snippet = searchSnippet(barcode);
        if (!StringUtils.hasText(snippet) || ERROR_MARKER.matcher(snippet).find()) {
            log.info("🔍 No {} product found for barcode: {}", SOURCE_NAME, barcode);
            return Optional.empty();
        }

        Matcher link = RESULT_LINK.matcher(snippet);
        if (!link.find()) {
            log.warn("⚠️ {} returned an unrecognized snippet for barcode {}", SOURCE_NAME, barcode);
            return Optional.empty();
        }
        String productUrl = link.group(1);
        String title = link.group(2).trim();

        // the search is fuzzy — without the page's own product code there
        // is no proof the hit is the queried product, so it is rejected
        String page = fetchProductPage(productUrl);
        if (page == null) {
            log.warn("⚠️ {} product page unavailable, cannot verify barcode {} — rejecting hit",
                    SOURCE_NAME, barcode);
            return Optional.empty();
        }
        Matcher code = PRODUCT_CODE.matcher(page);
        if (!code.find() || !barcode.equals(code.group(1))) {
            log.warn("⚠️ {} product {} carries code {} instead of barcode {} — rejecting hit",
                    SOURCE_NAME, productUrl, code.group(1), barcode);
            return Optional.empty();
        }

        String image = ogImage(page);
        if (image == null) {
            image = snippetImage(snippet);
        }
        return Optional.of(new ExternalProductDto(title, priceRial(page), image, mpn(page)));
    }

    /** package-private seams so unit tests can stub the HTTP calls. */
    protected String searchSnippet(String barcode) {
        return httpClient.post()
                .uri(baseUrl + "/search?title=" + barcode + "&catid=0&catseo=&ajax=ok")
                .retrieve()
                .body(String.class);
    }

    protected String fetchProductPage(String productUrl) {
        return httpClient.get()
                .uri(URI.create(productUrl))
                .retrieve()
                .body(String.class);
    }

    /** meta product_price is Toman; 0 means out of stock / no price. */
    private Long priceRial(String page) {
        Matcher m = PRICE_META.matcher(page);
        if (!m.find()) {
            return null;
        }
        long toman = Long.parseLong(m.group(1));
        return toman > 0 ? toman * 10 : null;
    }

    private String ogImage(String page) {
        Matcher m = OG_IMAGE.matcher(page);
        return m.find() ? m.group(1) : null;
    }

    private String snippetImage(String snippet) {
        Matcher m = SNIPPET_IMAGE.matcher(snippet);
        return m.find() ? m.group(1) : null;
    }

    private Long mpn(String page) {
        Matcher m = MPN.matcher(page);
        return m.find() ? Long.parseLong(m.group(1)) : null;
    }

    private static SimpleClientHttpRequestFactory requestFactory() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(5));
        factory.setReadTimeout(Duration.ofSeconds(10));
        return factory;
    }
}
