package com.stockguard.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * کوثرپلازا (kowsarplaza.com) — mweb-theme shop that publishes no SKU on its
 * product pages (no JSON-LD Product block); instead its catalog convention
 * appends the product barcode to the title. A hit therefore only counts when
 * the fetched page's own &lt;title&gt; carries the exact queried barcode.
 * Prices are Toman with '.' thousand separators; the numeric product id sits
 * in the body's postid-NNNN class.
 */
@Slf4j
@Component
public class KowsarplazaSource extends MwebShopSource {

    public static final String SOURCE_NAME = "KOWSARPLAZA";

    private static final Pattern TITLE_TAG = Pattern.compile("<title>([^<]*)</title>");
    private static final Pattern POSTID = Pattern.compile("postid-(\\d+)");
    private static final Pattern SINGLE_PRICE_BLOCK =
            Pattern.compile("class=\"price single_price\">.*?</p>", Pattern.DOTALL);
    private static final Pattern MAIN_IMAGE_TAG = Pattern.compile("<img[^>]*woocommerce-main-image[^>]*>");
    private static final Pattern DATA_SRC = Pattern.compile("data-src=\"([^\"]+)\"");
    private static final Pattern SRC = Pattern.compile("\\ssrc=\"([^\"]+)\"");

    public KowsarplazaSource(@Value("${kowsarplaza.api.base-url}") String baseUrl) {
        super(SOURCE_NAME, baseUrl);
    }

    @Override
    protected Optional<ExternalProductDto> parseHit(
            String snippetTitle, String productUrl, String snippet, String page, String barcode) {
        if (!carriesBarcode(snippetTitle, page, barcode)) {
            log.warn("⚠️ {} product {} does not carry barcode {} in its title — rejecting hit",
                    SOURCE_NAME, productUrl, barcode);
            return Optional.empty();
        }

        Long price = null;
        String image = null;
        Long id = null;
        if (page != null) {
            price = singlePriceRial(page);
            image = https(mainImage(page));
            Matcher postid = POSTID.matcher(page);
            if (postid.find()) {
                id = Long.parseLong(postid.group(1));
            }
        }
        if (price == null) {
            // snippet amounts are Toman too
            Long toman = lastSnippetAmount(snippet);
            price = toman != null ? toman * 10 : null;
        }
        if (image == null) {
            image = https(snippetImage(snippet));
        }
        if (id == null) {
            id = trailingSlugId(productUrl);
        }
        return Optional.of(new ExternalProductDto(
                stripTrailingBarcode(snippetTitle, barcode), price, image, id));
    }

    private boolean carriesBarcode(String snippetTitle, String page, String barcode) {
        if (page == null) {
            return snippetTitle.contains(barcode);
        }
        Matcher title = TITLE_TAG.matcher(page);
        return title.find() ? title.group(1).contains(barcode) : page.contains(barcode);
    }

    /** sale price (last bdi) of the main product's price block, Toman → Rial. */
    private Long singlePriceRial(String page) {
        Matcher block = SINGLE_PRICE_BLOCK.matcher(page);
        if (!block.find()) {
            return null;
        }
        String html = block.group();
        Long amount = null;
        Matcher bdi = BDI_AMOUNT.matcher(html);
        while (bdi.find()) {
            amount = parseAmount(bdi.group(1));
        }
        if (amount == null) {
            return null;
        }
        return html.contains("تومان") ? amount * 10 : amount;
    }

    /** the gallery image; lazy-loaded themes keep the real URL in data-src. */
    private String mainImage(String page) {
        Matcher tag = MAIN_IMAGE_TAG.matcher(page);
        if (!tag.find()) {
            return null;
        }
        String img = tag.group();
        Matcher url = DATA_SRC.matcher(img);
        if (url.find()) {
            return url.group(1);
        }
        url = SRC.matcher(img);
        return url.find() ? url.group(1) : null;
    }

    /** titles end with the barcode — useless noise in a product name. */
    private String stripTrailingBarcode(String title, String barcode) {
        if (title == null) {
            return null;
        }
        String cleaned = title.trim();
        if (cleaned.endsWith(barcode)) {
            cleaned = cleaned.substring(0, cleaned.length() - barcode.length())
                    .replaceAll("[\\s\\-–|،,]+$", "")
                    .trim();
        }
        return cleaned;
    }

    /** the shop serves some image URLs over http while being https itself. */
    private String https(String url) {
        return url != null && url.startsWith("http://")
                ? "https://" + url.substring("http://".length())
                : url;
    }
}
