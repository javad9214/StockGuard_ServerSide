package com.stockguard.client;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Plain unit test over the package-private HTTP seams — no real network.
 * The HTML fixtures were captured verbatim from martestan.com (live-search
 * snippet + product page metas and the jQuery کد محصول snippet; the page
 * fixtures keep only the parts the parser reads).
 */
class MartestanSourceTest {

    private static final String SHAMPOO_BARCODE = "6260031000026";

    private static final String SHAMPOO_SNIPPET = """
            <div class="advancedSearchScroll">
                    <h6>محصولات</h6>
                <div id="advancedSearchProducts">
                    <ul class="list-unstyled">
                                            <li>
                            <a href="https://www.martestan.com/product/شامپو-سدر-صحت-300-میلی-لیتر">
                                                                <img src="https://www.martestan.com/upload/thumb2/product/1625755777-134724.jpg" width="50" height="50" alt="شامپو سدر صحت 300 میلی لیتر"/>
                                                            </a>
                            <a href="https://www.martestan.com/product/شامپو-سدر-صحت-300-میلی-لیتر" class="title">شامپو سدر صحت 300 میلی لیتر</span></a>
                        </li>
                                            </ul>
                </div>
                    <a href="https://www.martestan.com/product?keyword=6260031000026" class="btn advanceMoreBtn">ادامه</a>
                </div>
            """;

    private static final String SHAMPOO_PAGE = """
            <html><head>
            <meta property="og:image" content="https://www.martestan.com/upload/product/1625755777-134724.jpg"/>
            <meta name="product_price" content="75000"/>
            <meta name="product_old_price" content="75000"/>
            <meta name="availability" content="instock"/>
            <script type="application/ld+json">{"@context":"schema.org","@type":"Product","sku":"تعداد","mpn":"2747"}</script>
            </head><body>
            <script>$(document).ready(function () {
                $('.change-code').show();
                $('.change-code').html("کد محصول : <span>6260031000026</span>");
            });</script>
            <div class="productPage__price--new"> ۷۵,۰۰۰ <span>تومان</span></div>
            </body></html>
            """;

    /** out-of-stock hand wash — price meta is 0, code still verifies. */
    private static final String HANDWASH_SNIPPET = """
            <div class="advancedSearchScroll">
                    <h6>محصولات</h6>
                <div id="advancedSearchProducts">
                    <ul class="list-unstyled">
                                            <li>
                            <a href="https://www.martestan.com/product/مایع-دستشویی-هندولوژی-کرمی-با-رایحه-توت-500-میلی-لیتر">
                                                                <img src="https://www.martestan.com/upload/thumb2/product/1652256777-6264707011248.jpg" width="50" height="50" alt="مایع دستشویی کرمی هندولوژی با رایحه توت 500 میلی لیتر"/>
                                                            </a>
                            <a href="https://www.martestan.com/product/مایع-دستشویی-هندولوژی-کرمی-با-رایحه-توت-500-میلی-لیتر" class="title">مایع دستشویی کرمی هندولوژی با رایحه توت 500 میلی لیتر</span></a>
                        </li>
                                            </ul>
                </div>
                    <a href="https://www.martestan.com/product?keyword=6264707011248" class="btn advanceMoreBtn">ادامه</a>
                </div>
            """;

    private static final String HANDWASH_PAGE = """
            <html><head>
            <meta property="og:image" content="https://www.martestan.com/upload/product/1652256777-6264707011248.jpg"/>
            <meta name="product_price" content="0"/>
            <meta name="product_old_price" content="0"/>
            <meta name="availability" content="outofstock"/>
            <script type="application/ld+json">{"@context":"schema.org","@type":"Product","sku":"تعداد","mpn":"10458"}</script>
            </head><body>
            <script>$(document).ready(function () {
                $('.change-code').show();
                $('.change-code').html("کد محصول : <span>6264707011248</span>");
            });</script>
            <div class="alert alert-danger">ناموجود</div>
            </body></html>
            """;

    private MartestanSource sourceReturning(String snippet, String page) {
        return new MartestanSource("https://www.martestan.com") {
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
    void parsesProductCodeVerifiedPriceAndId() {
        Optional<ExternalProductDto> found = sourceReturning(SHAMPOO_SNIPPET, SHAMPOO_PAGE)
                .findByBarcode(SHAMPOO_BARCODE);

        assertThat(found).isPresent();
        ExternalProductDto p = found.get();
        assertThat(p.name()).isEqualTo("شامپو سدر صحت 300 میلی لیتر");
        assertThat(p.priceRial()).isEqualTo(750000L); // meta 75000 Toman ×10
        assertThat(p.imageUrl()).isEqualTo("https://www.martestan.com/upload/product/1625755777-134724.jpg");
        assertThat(p.externalId()).isEqualTo(2747L);  // JSON-LD mpn
    }

    @Test
    void emptyWhenShopReportsError() {
        assertThat(sourceReturning(
                "<div class=\"advancedSearchScroll\">\n <div class=\"error\">نتیجه ای یافت نشد</div>\n </div>",
                null).findByBarcode("0000000000000")).isEmpty();
    }

    @Test
    void rejectsHitWhoseProductCodeDiffers() {
        // the search is fuzzy — a page whose کد محصول is another number is a miss
        String otherCode = SHAMPOO_PAGE.replace(SHAMPOO_BARCODE, "6269999999999");
        assertThat(sourceReturning(SHAMPOO_SNIPPET, otherCode)
                .findByBarcode(SHAMPOO_BARCODE)).isEmpty();
    }

    @Test
    void emptyWhenProductPageUnavailable() {
        // without the page there is no proof the fuzzy hit is the right product
        assertThat(sourceReturning(SHAMPOO_SNIPPET, null)
                .findByBarcode(SHAMPOO_BARCODE)).isEmpty();
    }

    @Test
    void nullPriceWhenOutOfStock() {
        Optional<ExternalProductDto> found = sourceReturning(HANDWASH_SNIPPET, HANDWASH_PAGE)
                .findByBarcode("6264707011248");

        assertThat(found).isPresent();
        ExternalProductDto p = found.get();
        assertThat(p.name()).isEqualTo("مایع دستشویی کرمی هندولوژی با رایحه توت 500 میلی لیتر");
        assertThat(p.priceRial()).isNull();          // meta 0 → no price
        assertThat(p.imageUrl()).contains("6264707011248.jpg");
        assertThat(p.externalId()).isEqualTo(10458L);
    }
}
