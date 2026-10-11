package com.stockguard.client;

import com.stockguard.data.dto.roja.RojaProductDto;
import com.stockguard.data.dto.roja.RojaSearchResponseDto;
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
 * فروشگاه اینترنتی روژا (rojashop.com) — cosmetics/hygiene shop with its
 * own JSON ajax search: POST /api/ajax_search with form body
 * term=&lt;barcode&gt;. The endpoint answers only requests carrying
 * same-origin headers (Origin/Referer/X-Requested-With) and a real
 * browser User-Agent — a bot-style UA got 403 from datacenter IPs while
 * the same request browsed as Chrome passed. The response carries no
 * barcode field — the shop names each product image after its barcode
 * (…/&lt;internal id&gt;/&lt;BARCODE&gt;.jpg), and only a hit whose image carries the
 * queried barcode is accepted. The product-page JSON-LD price is stale
 * on this shop, so the live sale price is read from the page HTML
 * (تومان with Persian digits → ×10 Rial).
 */
@Slf4j
@Component
public class RojashopSource implements ExternalBarcodeSource {

    public static final String SOURCE_NAME = "ROJASHOP";

    private static final String X_REQUESTED_WITH = "X-Requested-With";

    private static final Pattern IMAGE_BARCODE = Pattern.compile("/(\\d{13})[-.]");
    private static final Pattern SALE_PRICE =
            Pattern.compile("<strong class=\"text-black[^\"]*\">\\s*([\\d٠-٩۰-۹,]+)\\s*<span>\\s*تومان");

    private final String baseUrl;

    // the shop's middleware rejects bot-looking clients from datacenter
    // IPs ("Mozilla/5.0 (compatible; …)" got 403 while the same request
    // with a real browser UA passed) — so this source browses as Chrome
    private final RestClient httpClient = RestClient.builder()
            .requestFactory(requestFactory())
            .defaultHeader(HttpHeaders.USER_AGENT,
                    "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 "
                            + "(KHTML, like Gecko) Chrome/126.0 Safari/537.36")
            .defaultHeader(HttpHeaders.ACCEPT_LANGUAGE, "fa-IR,fa;q=0.9,en;q=0.8")
            .build();

    public RojashopSource(@Value("${rojashop.api.base-url}") String baseUrl) {
        this.baseUrl = baseUrl;
    }

    @Override
    public String name() {
        return SOURCE_NAME;
    }

    @Override
    public Optional<ExternalProductDto> findByBarcode(String barcode) {
        RojaSearchResponseDto response = searchProducts(barcode);
        if (response == null || !Boolean.TRUE.equals(response.getStatus())
                || response.getData() == null
                || response.getData().getProducts() == null
                || response.getData().getProducts().isEmpty()) {
            log.info("🔍 No {} product found for barcode: {}", SOURCE_NAME, barcode);
            return Optional.empty();
        }

        RojaProductDto product = response.getData().getProducts().get(0);
        if (!imageCarriesBarcode(product.getImage(), barcode)) {
            log.warn("⚠️ {} product {} image does not carry barcode {} — rejecting hit",
                    SOURCE_NAME, product.getUrl(), barcode);
            return Optional.empty();
        }

        String page = fetchProductPage(product.getUrl());
        return Optional.of(new ExternalProductDto(
                product.getTitle(),
                salePriceRial(page),
                product.getImage(),
                product.getId()));
    }

    /** package-private seams so unit tests can stub the HTTP calls. */
    protected RojaSearchResponseDto searchProducts(String barcode) {
        return httpClient.post()
                .uri(baseUrl + "/api/ajax_search")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    // digits-only term, no form-encoding concerns
                .body("term=" + barcode)
                .header(HttpHeaders.ORIGIN, baseUrl)
                .header(HttpHeaders.REFERER, baseUrl + "/")
                .header(X_REQUESTED_WITH, "XMLHttpRequest")
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .body(RojaSearchResponseDto.class);
    }

    protected String fetchProductPage(String productUrl) {
        return httpClient.get()
                .uri(URI.create(productUrl))
                .retrieve()
                .body(String.class);
    }

    private boolean imageCarriesBarcode(String imageUrl, String barcode) {
        if (!StringUtils.hasText(imageUrl)) {
            return false;
        }
        Matcher m = IMAGE_BARCODE.matcher(imageUrl);
        while (m.find()) {
            if (barcode.equals(m.group(1))) {
                return true;
            }
        }
        return false;
    }

    /** the live sale price; JSON-LD on this shop holds a stale value. */
    private Long salePriceRial(String page) {
        if (page == null) {
            return null;
        }
        Matcher m = SALE_PRICE.matcher(page);
        if (!m.find()) {
            return null;
        }
        String digits = toAsciiDigits(m.group(1)).replace(",", "").trim();
        try {
            long toman = Long.parseLong(digits);
            return toman > 0 ? toman * 10 : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String toAsciiDigits(String s) {
        StringBuilder sb = new StringBuilder(s.length());
        for (char c : s.toCharArray()) {
            switch (c) {
                case '۰', '٠' -> sb.append('0');
                case '۱', '١' -> sb.append('1');
                case '۲', '٢' -> sb.append('2');
                case '۳', '٣' -> sb.append('3');
                case '۴', '٤' -> sb.append('4');
                case '۵', '٥' -> sb.append('5');
                case '۶', '٦' -> sb.append('6');
                case '۷', '٧' -> sb.append('7');
                case '۸', '٨' -> sb.append('8');
                case '۹', '٩' -> sb.append('9');
                default -> sb.append(c);
            }
        }
        return sb.toString();
    }

    private static SimpleClientHttpRequestFactory requestFactory() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(5));
        factory.setReadTimeout(Duration.ofSeconds(10));
        return factory;
    }
}
