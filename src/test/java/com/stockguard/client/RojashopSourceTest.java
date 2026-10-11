package com.stockguard.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.stockguard.data.dto.roja.RojaSearchResponseDto;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Plain unit test over the package-private HTTP seams — no real network.
 * The JSON fixture was captured verbatim from rojashop.com's live
 * /api/ajax_search; the page fixture condenses the captured product page
 * to the parts the parser reads (strikethrough old price, text-black sale
 * price, the installment span as a تومان trap).
 */
class RojashopSourceTest {

    private static final String BARCODE = "6260147707710";

    private static final String SEARCH_JSON = """
            {"status":true,"data":{"products":[{"id":24020,"title":"مایع دستشویی هرباسنس مدل Mountain Dew آردن","image":"https://images.rojashop.com/pr:sharp/rs:fill:200:0:0/plain/s3://uploads/products/behdashti/brand_20430/35542/6260147707710.jpg","url":"https://rojashop.com/product/%D9%85%D8%A7%DB%8C%D8%B9-%D8%AF%D8%B3%D8%AA%D8%B4%D9%88%DB%8C%DB%8C-%D9%87%D8%B1%D8%A8%D8%A7%D8%B3%D9%86%D8%B3-%D9%85%D8%AF%D9%84-mountain-dew"}],"cats":[],"brands":[],"auto_complete":"","term":"6260147707710"},"search_page":"/"}
            """;

    private static final String PRODUCT_PAGE = """
            <div class="product-price-box mt-2 mt-lg-0">
            <small class="text-muted">(قیمت با ارزش افزوده)</small>
            <strong class="text-info h6 text-decoration-line-through fw-normal d-block">۲۴۹,۹۲۰ <span>تومان</span></strong>
            <strong class="text-black h5 mt-2 fw-normal d-block"> ۱۴۹,۹۵۲ <span>تومان</span></strong>
            <div class="product-installment"> <span>۴</span> <span>قسط</span> <span>37,488</span> <span>تومانی با اسنپ پی</span> </div>
            </div>
            """;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private RojashopSource sourceReturning(String searchJson, String page) throws Exception {
        RojaSearchResponseDto response = objectMapper.readValue(searchJson, RojaSearchResponseDto.class);
        return new RojashopSource("https://rojashop.com") {
            @Override
            protected RojaSearchResponseDto searchProducts(String barcode) {
                return response;
            }

            @Override
            protected String fetchProductPage(String productUrl) {
                return page;
            }
        };
    }

    @Test
    void parsesTitleImageIdAndPersianDigitTomanPrice() throws Exception {
        Optional<ExternalProductDto> found = sourceReturning(SEARCH_JSON, PRODUCT_PAGE)
                .findByBarcode(BARCODE);

        assertThat(found).isPresent();
        ExternalProductDto p = found.get();
        assertThat(p.name()).isEqualTo("مایع دستشویی هرباسنس مدل Mountain Dew آردن");
        assertThat(p.priceRial()).isEqualTo(1499520L); // ۱۴۹,۹۵۲ Toman, ×10
        assertThat(p.imageUrl()).contains("6260147707710.jpg");
        assertThat(p.externalId()).isEqualTo(24020L);
    }

    @Test
    void emptyWhenShopReportsNoProducts() throws Exception {
        assertThat(sourceReturning("{\"status\":true,\"data\":{\"products\":[]}}", null)
                .findByBarcode(BARCODE)).isEmpty();
    }

    @Test
    void rejectsHitWhoseImageLacksBarcode() throws Exception {
        // the product image is named after another barcode — must be a miss
        String otherBarcode = SEARCH_JSON.replace(BARCODE, "6260147707796");
        assertThat(sourceReturning(otherBarcode, PRODUCT_PAGE)
                .findByBarcode(BARCODE)).isEmpty();
    }

    @Test
    void nullPriceWhenPageUnavailable() throws Exception {
        // page fetch failed — title/image/id still answer, price stays null
        Optional<ExternalProductDto> found = sourceReturning(SEARCH_JSON, null)
                .findByBarcode(BARCODE);

        assertThat(found).isPresent();
        assertThat(found.get().name()).isEqualTo("مایع دستشویی هرباسنس مدل Mountain Dew آردن");
        assertThat(found.get().priceRial()).isNull();
        assertThat(found.get().externalId()).isEqualTo(24020L);
    }

    @Test
    void nullPriceWhenPageHasNoSalePriceMarkup() throws Exception {
        // e.g. out-of-stock rows render no price block
        String barePage = "<html><body>ناموجود</body></html>";
        Optional<ExternalProductDto> found = sourceReturning(SEARCH_JSON, barePage)
                .findByBarcode(BARCODE);

        assertThat(found).isPresent();
        assertThat(found.get().priceRial()).isNull();
    }
}
