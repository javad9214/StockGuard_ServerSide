package com.stockguard.client;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Plain unit test over the package-private HTTP seams — no real network.
 * The HTML/JSON fixtures were captured verbatim from 4soocenter.ir
 * (mweb admin-ajax snippet + product-page JSON-LD).
 */
class SoocenterSourceTest {

    private static final String SNIPPET = """
            <div class="ajax_search_list vertical_scroll_css">
                <div class="post_with_thumb">
                    <div class="post-thumb"></div>
                    <div class="inner">
                        <a href="https://4soocenter.ir/product/%d9%85%d8%ad%d8%b5%d9%88%d9%84-4928/" title="شیرین عسل-شوکو کیک (وانیل)-40 گرمی" target="_blank">شیرین عسل-شوکو کیک (وانیل)-40 گرمی</a>
                        <span class="price">
                            <del><span class="woocommerce-Price-amount amount"><bdi>200,000&nbsp;<span>ریال</span></bdi></span></del>
                            <ins><span class="woocommerce-Price-amount amount"><bdi>185,000&nbsp;<span>ریال</span></bdi></span></ins>
                        </span>
                    </div>
                </div>
            </div>
            """;

    private static final String PRODUCT_PAGE = """
            <script type="application/ld+json">{"@context":"https://schema.org/","@graph":[
            {"@type":"BreadcrumbList","itemListElement":[{"@type":"ListItem","name":"Home"},{"@type":"ListItem","name":"محصولات"}]},
            {"@type":"Product","name":"شیرین عسل-شوکو کیک (وانیل)-40 گرمی","description":null,
             "sku":"6261149137093","mpn":null,"aggregateRating":{"@type":"AggregateRating","ratingValue":"4"},
             "image":["https://4soocenter.ir/wp-content/uploads/2024/03/choco.jpg"],
             "offers":{"@type":"Offer","price":"185000","priceCurrency":"IRR",
             "url":"https://4soocenter.ir/product/%d9%85%d8%ad%d8%b5%d9%88%d9%84-4928/"}}]}</script>
            """;

    private SoocenterSource sourceReturning(String snippet, String page) {
        return new SoocenterSource("https://4soocenter.ir") {
            @Override
            protected String searchSnippet(String barcode) {
                return snippet;
            }

            @Override
            protected String fetchProductPage(String productUrl) {
                return page;
            }
        };
    }

    @Test
    void parsesJsonLdProductAndVerifiesSku() {
        Optional<ExternalProductDto> found = sourceReturning(SNIPPET, PRODUCT_PAGE)
                .findByBarcode("6261149137093");

        assertThat(found).isPresent();
        ExternalProductDto p = found.get();
        assertThat(p.name()).isEqualTo("شیرین عسل-شوکو کیک (وانیل)-40 گرمی");
        assertThat(p.priceRial()).isEqualTo(185000L);          // IRR — no conversion
        assertThat(p.imageUrl()).isEqualTo("https://4soocenter.ir/wp-content/uploads/2024/03/choco.jpg");
        assertThat(p.externalId()).isEqualTo(4928L);           // from محصول-4928 slug
    }

    @Test
    void emptyWhenShopReportsNotFound() {
        assertThat(sourceReturning(
                "<div class=\"header-search-not-found\"> نتیجه‌ای یافت نشد </div>", null)
                .findByBarcode("1111111111111")).isEmpty();
    }

    @Test
    void rejectsHitWhoseSkuDiffersFromBarcode() {
        // the page's SKU belongs to another product — must be treated as a miss
        String wrongSku = PRODUCT_PAGE.replace("6261149137093", "6269999999999");
        assertThat(sourceReturning(SNIPPET, wrongSku)
                .findByBarcode("6261149137093")).isEmpty();
    }

    @Test
    void fallsBackToSnippetPriceWhenJsonLdHasNone() {
        String noPricePage = PRODUCT_PAGE.replaceAll("\"price\":\"185000\",", "");
        Optional<ExternalProductDto> found = sourceReturning(SNIPPET, noPricePage)
                .findByBarcode("6261149137093");

        assertThat(found).isPresent();
        // last <bdi> in the snippet = the 185,000 sale price (Rial)
        assertThat(found.get().priceRial()).isEqualTo(185000L);
    }

    @Test
    void convertsTomanCurrencyToRial() {
        String tomanPage = PRODUCT_PAGE.replace("\"priceCurrency\":\"IRR\"", "\"priceCurrency\":\"TMN\"");
        Optional<ExternalProductDto> found = sourceReturning(SNIPPET, tomanPage)
                .findByBarcode("6261149137093");

        assertThat(found).isPresent();
        assertThat(found.get().priceRial()).isEqualTo(1850000L); // ×10
    }

    @Test
    void toleratesMissingProductPage() {
        // page fetch failed — snippet data still answers (no image, snippet price)
        Optional<ExternalProductDto> found = sourceReturning(SNIPPET, null)
                .findByBarcode("6261149137093");

        assertThat(found).isPresent();
        assertThat(found.get().name()).isEqualTo("شیرین عسل-شوکو کیک (وانیل)-40 گرمی");
        assertThat(found.get().priceRial()).isEqualTo(185000L);
        assertThat(found.get().imageUrl()).isNull();
        assertThat(found.get().externalId()).isEqualTo(4928L);
    }
}
