package com.stockguard.client;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Plain unit test over the package-private HTTP seams — no real network.
 * The HTML fixtures were captured verbatim from kowsarplaza.com (mweb
 * admin-ajax snippet + single-product page, condensed to the parts the
 * parser reads: title, postid body class, single_price block, gallery img).
 */
class KowsarplazaSourceTest {

    private static final String BARCODE = "6261149137093";

    private static final String SNIPPET = """
            <div class="ajax_search_list horizontal_scroll_css"><div class="post_with_thumb"><div class="post-thumb"><img width="300" height="300" src="http://kowsarplaza.com/wp-content/uploads/2021/11/1-5.gif" class="attachment-mini_blog size-mini_blog wp-post-image" alt="شوکو کیک دوقلو روکشدار آلبینا 40گرم شیرین عسل 6261149137093" decoding="async" /></div><div class="inner"><a href="https://kowsarplaza.com/product/%d8%b4%d9%88%da%a9%d9%88-%da%a9%db%8c%da%a9-%d8%af%d9%88%d9%82%d9%84%d9%88-%d8%b1%d9%88%da%a9%d8%b4%d8%af%d8%a7%d8%b1-%d8%a2%d9%84%d8%a8%db%8c%d9%86%d8%a7-40%da%af%d8%b1%d9%85-%d8%b4%db%8c%d8%b1%db%8c/" title="شوکو کیک دوقلو روکشدار آلبینا 40گرم شیرین عسل 6261149137093" target="_blank">شوکو کیک دوقلو روکشدار آلبینا 40گرم شیرین عسل 6261149137093</a><span class="price"><del aria-hidden="true"><span class="woocommerce-Price-amount amount"><bdi>25.000<span class="woocommerce-Price-currencySymbol">تومان</span></bdi></span></del> <span class="screen-reader-text">قیمت اصلی: 25.000تومان بود.</span><ins aria-hidden="true"><span class="woocommerce-Price-amount amount"><bdi>24.200<span class="woocommerce-Price-currencySymbol">تومان</span></bdi></span></ins><span class="screen-reader-text">قیمت فعلی: 24.200تومان.</span></span></div></div></div>
            """;

    private static final String PRODUCT_PAGE = """
            <!DOCTYPE html><html lang="fa-IR"><head><title>شوکو کیک دوقلو روکشدار آلبینا 40گرم شیرین عسل 6261149137093 &#8211; کوثرپلازا</title></head>
            <body class="rtl wp-singular product-template-default single single-product postid-27898 wp-embed-responsive wp-theme-mweb-digiland-pro theme-mweb-digiland-pro woocommerce woocommerce-page">
            <div class="product-detail-area">
            <img width="600" height="600" src="https://kowsarplaza.com/wp-content/uploads/2021/11/1-5.gif" class="woocommerce-main-image" alt="شوکو کیک دوقلو روکشدار آلبینا 40گرم شیرین عسل 6261149137093" data-caption="" data-src="https://kowsarplaza.com/wp-content/uploads/2021/11/1-5.gif" />
            <p class="price single_price"><del aria-hidden="true"><span class="woocommerce-Price-amount amount"><bdi>25.000<span class="woocommerce-Price-currencySymbol">تومان</span></bdi></span></del> <span class="screen-reader-text">قیمت اصلی: 25.000تومان بود.</span><ins aria-hidden="true"><span class="woocommerce-Price-amount amount"><bdi>24.200<span class="woocommerce-Price-currencySymbol">تومان</span></bdi></span></ins><span class="screen-reader-text">قیمت فعلی: 24.200تومان.</span></p>
            </div>
            <h2 class="woocommerce-loop-product__title">محصول مرتبط</h2>
            <span class="price"><span class="woocommerce-Price-amount amount"><bdi>112.700<span class="woocommerce-Price-currencySymbol">تومان</span></bdi></span></span>
            </body></html>
            """;

    private KowsarplazaSource sourceReturning(String snippet, String page) {
        return new KowsarplazaSource("https://kowsarplaza.com") {
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
    void parsesPageDetailsStripsBarcodeFromTitleAndDoublesTomanPrice() {
        Optional<ExternalProductDto> found = sourceReturning(SNIPPET, PRODUCT_PAGE)
                .findByBarcode(BARCODE);

        assertThat(found).isPresent();
        ExternalProductDto p = found.get();
        assertThat(p.name()).isEqualTo("شوکو کیک دوقلو روکشدار آلبینا 40گرم شیرین عسل");
        assertThat(p.priceRial()).isEqualTo(242000L); // 24.200 Toman, '.' separators, ×10
        assertThat(p.imageUrl()).isEqualTo("https://kowsarplaza.com/wp-content/uploads/2021/11/1-5.gif");
        assertThat(p.externalId()).isEqualTo(27898L);  // body postid-27898
    }

    @Test
    void emptyWhenShopReportsNotFound() {
        assertThat(sourceReturning(
                "<div class=\"header-search-not-found\"> نتیجه‌ای یافت نشد </div>", null)
                .findByBarcode(BARCODE)).isEmpty();
    }

    @Test
    void rejectsHitWhosePageTitleLacksBarcode() {
        // the page belongs to another product — must be treated as a miss
        String otherProduct = PRODUCT_PAGE.replace(BARCODE, "6269999999999");
        assertThat(sourceReturning(SNIPPET, otherProduct)
                .findByBarcode(BARCODE)).isEmpty();
    }

    @Test
    void fallsBackToSnippetWhenPageUnavailable() {
        // page fetch failed — the snippet title still proves the barcode
        Optional<ExternalProductDto> found = sourceReturning(SNIPPET, null)
                .findByBarcode(BARCODE);

        assertThat(found).isPresent();
        ExternalProductDto p = found.get();
        assertThat(p.name()).isEqualTo("شوکو کیک دوقلو روکشدار آلبینا 40گرم شیرین عسل");
        assertThat(p.priceRial()).isEqualTo(242000L); // snippet 24.200 Toman ×10
        assertThat(p.imageUrl()).isEqualTo("https://kowsarplaza.com/wp-content/uploads/2021/11/1-5.gif");
        assertThat(p.externalId()).isNull();           // slug carries no trailing id
    }

    @Test
    void rejectsSnippetWithoutBarcodeWhenPageUnavailable() {
        String noBarcodeInTitle = SNIPPET.replaceAll(BARCODE, "6269999999999");
        assertThat(sourceReturning(noBarcodeInTitle, null)
                .findByBarcode(BARCODE)).isEmpty();
    }
}
