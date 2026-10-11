package com.stockguard.service;

import com.stockguard.client.DaryamartClient;
import com.stockguard.client.KowsarplazaSource;
import com.stockguard.client.SehSootehClient;
import com.stockguard.client.SoocenterSource;
import com.stockguard.client.TisaMarketClient;
import com.stockguard.data.dto.barcode.response.BarcodeProductResponseDTO;
import com.stockguard.data.dto.sepehr.SepehrSearchResponseDto;
import com.stockguard.data.entity.CatalogProduct;
import com.stockguard.repository.CatalogProductRepository;
import com.stockguard.service.impl.BarcodeLookupServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Plain unit test: the payload below was captured verbatim from the
 * live GetProductsSearch endpoint (same envelope on every Sepehr shop:
 * Daryamart, Seh Sooteh, Tisa Market), so it also pins the JSON
 * deserialization (field names, isSuccess mapping, relative imageAddress).
 */
class BarcodeLookupServiceImplTest {

    private static final String RESPONSE_JSON = """
            {
                "data": {
                    "source": [
                        {
                            "id": 2413292,
                            "title": "",
                            "name": "بیسکویت شکو چیپس شکلاتی سلامت",
                            "slug": "بیسکویت-شکو-چیپس-شکلاتی-سلامت",
                            "image": 0,
                            "imageAddress": "/host/shabazi/2024/12/97863679310338.png",
                            "imageAddress2": "",
                            "imageAlt": "بیسکویت شکو چیپس شکلاتی سلامت",
                            "groupId": 62384,
                            "price": 132900,
                            "shopId": 26697
                        }
                    ],
                    "currentPage": 1,
                    "totalPages": 1,
                    "pageSize": 50,
                    "totalCount": 1,
                    "hasPrevious": false,
                    "hasNext": false
                },
                "isSuccess": true,
                "statusCode": 200,
                "message": "عملیات با موفقیت انجام شد"
            }
            """;

    private static final String DARYAMART_BASE_URL = "https://daryamart.ir";
    private static final String SEHSOOTEH_BASE_URL = "https://3sooteh.com";
    private static final String TISAMARKET_BASE_URL = "https://tisamarket.co";

    private final ObjectMapper objectMapper = new ObjectMapper();

    private SepehrSearchResponseDto successfulResponse;

    @BeforeEach
    void setUp() throws Exception {
        successfulResponse = objectMapper.readValue(RESPONSE_JSON, SepehrSearchResponseDto.class);
    }

    private interface FakeClients {
        static DaryamartClient daryamart(SepehrSearchResponseDto response) {
            return new DaryamartClient() {
                @Override
                public SepehrSearchResponseDto searchProducts(String key, int pageNumber, int pageSize) {
                    return response;
                }
            };
        }

        static SehSootehClient sehSooteh(SepehrSearchResponseDto response) {
            return new SehSootehClient() {
                @Override
                public SepehrSearchResponseDto searchProducts(String key, int pageNumber, int pageSize) {
                    return response;
                }
            };
        }

        static TisaMarketClient tisaMarket(SepehrSearchResponseDto response) {
            return new TisaMarketClient() {
                @Override
                public SepehrSearchResponseDto searchProducts(String key, int pageNumber, int pageSize) {
                    return response;
                }
            };
        }
    }

    private BarcodeLookupServiceImpl serviceReturning(SepehrSearchResponseDto response) {
        return serviceReturning(response, imageStorageFailingWith("MinIO unavailable in unit test"));
    }

    private BarcodeLookupServiceImpl serviceReturning(
            SepehrSearchResponseDto response,
            com.stockguard.service.ImageStorageService imageStorage) {
        return serviceWithClients(
                FakeClients.daryamart(response),
                FakeClients.sehSooteh(null),
                FakeClients.tisaMarket(null),
                emptyCatalogRepository(),
                imageStorage);
    }

    private BarcodeLookupServiceImpl serviceWithClients(
            DaryamartClient daryamart,
            SehSootehClient sehSooteh,
            TisaMarketClient tisaMarket,
            CatalogProductRepository repo,
            com.stockguard.service.ImageStorageService imageStorage) {
        return serviceWithClients(daryamart, sehSooteh, tisaMarket, soocenterMissing(), kowsarplazaMissing(), repo, imageStorage);
    }

    private BarcodeLookupServiceImpl serviceWithClients(
            DaryamartClient daryamart,
            SehSootehClient sehSooteh,
            TisaMarketClient tisaMarket,
            SoocenterSource soocenter,
            CatalogProductRepository repo,
            com.stockguard.service.ImageStorageService imageStorage) {
        return serviceWithClients(daryamart, sehSooteh, tisaMarket, soocenter, kowsarplazaMissing(), repo, imageStorage);
    }

    private BarcodeLookupServiceImpl serviceWithClients(
            DaryamartClient daryamart,
            SehSootehClient sehSooteh,
            TisaMarketClient tisaMarket,
            SoocenterSource soocenter,
            KowsarplazaSource kowsarplaza,
            CatalogProductRepository repo,
            com.stockguard.service.ImageStorageService imageStorage) {
        BarcodeLookupServiceImpl impl = new BarcodeLookupServiceImpl(
                daryamart,
                sehSooteh,
                tisaMarket,
                soocenter,
                kowsarplaza,
                repo,
                org.mockito.Mockito.mock(com.stockguard.repository.CategoryRepository.class),
                org.mockito.Mockito.mock(com.stockguard.repository.SubcategoryRepository.class),
                imageStorage);
        ReflectionTestUtils.setField(impl, "daryamartBaseUrl", DARYAMART_BASE_URL);
        ReflectionTestUtils.setField(impl, "sehSootehBaseUrl", SEHSOOTEH_BASE_URL);
        ReflectionTestUtils.setField(impl, "tisamarketBaseUrl", TISAMARKET_BASE_URL);
        return impl;
    }

    /**
     * Storage that never succeeds: storeImage then falls back to the direct
     * shop URL, which is what the pre-MinIO assertions pin.
     */
    private com.stockguard.service.ImageStorageService imageStorageFailingWith(String reason) {
        com.stockguard.service.ImageStorageService storage =
                org.mockito.Mockito.mock(com.stockguard.service.ImageStorageService.class);
        org.mockito.Mockito.when(storage.storeFromUrl(org.mockito.ArgumentMatchers.anyString()))
                .thenThrow(new RuntimeException(reason));
        return storage;
    }


    /** mweb stubs that miss every barcode (chain tests only need them inert). */
    private SoocenterSource soocenterMissing() {
        return new SoocenterSource("https://4soocenter.ir") {
            @Override
            protected String searchSnippet(String barcode) {
                return "<div class=\"header-search-not-found\"> نتیجه‌ای یافت نشد </div>";
            }
        };
    }

    private KowsarplazaSource kowsarplazaMissing() {
        return new KowsarplazaSource("https://kowsarplaza.com") {
            @Override
            protected String searchSnippet(String barcode) {
                return "<div class=\"header-search-not-found\"> نتیجه‌ای یافت نشد </div>";
            }
        };
    }

    private CatalogProductRepository emptyCatalogRepository() {
        CatalogProductRepository repo = org.mockito.Mockito.mock(CatalogProductRepository.class);
        org.mockito.Mockito.when(repo.findByBarcodeAndIsActiveTrue(org.mockito.ArgumentMatchers.anyString()))
                .thenReturn(Optional.empty());
        // catalog-caching path: echo the saved entity so the post-save log doesn't NPE
        org.mockito.Mockito.when(repo.save(org.mockito.ArgumentMatchers.any(CatalogProduct.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        return repo;
    }

    private static SepehrSearchResponseDto emptyResponse() {
        SepehrSearchResponseDto empty = new SepehrSearchResponseDto();
        empty.setSuccess(true);
        empty.setData(new com.stockguard.data.dto.sepehr.SepehrPagedDataDto());
        empty.getData().setSource(java.util.List.of());
        return empty;
    }

    @Test
    void mapsFirstProductWithNameSellPriceAndAbsoluteImageUrl() {
        BarcodeLookupServiceImpl service = serviceReturning(successfulResponse);

        Optional<BarcodeProductResponseDTO> result = service.lookupByBarcode("6261145000407");

        assertThat(result).isPresent();
        assertThat(result.get().getName()).isEqualTo("بیسکویت شکو چیپس شکلاتی سلامت");
        assertThat(result.get().getSellPrice()).isEqualTo(1329000L); // toman 132900 converted to rial
        assertThat(result.get().getImageUrl())
                .isEqualTo(DARYAMART_BASE_URL + "/host/shabazi/2024/12/97863679310338.png");
    }

    @Test
    void deserializesEnvelopeFlags() {
        assertThat(successfulResponse.getSuccess()).isTrue();
        assertThat(successfulResponse.getData().getTotalCount()).isEqualTo(1L);
        assertThat(successfulResponse.getData().getSource()).hasSize(1);
    }

    @Test
    void returnsEmptyWhenApiReportsFailure() {
        successfulResponse.setSuccess(false);
        BarcodeLookupServiceImpl service = serviceReturning(successfulResponse);

        assertThat(service.lookupByBarcode("6261145000407")).isEmpty();
    }

    @Test
    void returnsEmptyWhenNoProductMatches() {
        successfulResponse.getData().setSource(java.util.List.of());
        BarcodeLookupServiceImpl service = serviceReturning(successfulResponse);

        assertThat(service.lookupByBarcode("0000000000000")).isEmpty();
    }

    @Test
    void returnsNullImageUrlWhenAddressIsBlank() throws Exception {
        successfulResponse.getData().getSource().get(0).setImageAddress("   ");
        BarcodeLookupServiceImpl service = serviceReturning(successfulResponse);

        Optional<BarcodeProductResponseDTO> result = service.lookupByBarcode("6261145000407");

        assertThat(result).isPresent();
        assertThat(result.get().getImageUrl()).isNull();
    }

    @Test
    void keepsAbsoluteImageUrlAsIs() {
        successfulResponse.getData().getSource().get(0).setImageAddress("https://cdn.example.com/img.png");
        BarcodeLookupServiceImpl service = serviceReturning(successfulResponse);

        assertThat(service.lookupByBarcode("6261145000407"))
                .map(BarcodeProductResponseDTO::getImageUrl)
                .contains("https://cdn.example.com/img.png");
    }

    @Test
    void returnsCompleteCatalogHitWithoutCallingShops() {
        CatalogProduct complete = CatalogProduct.builder()
                .id(7L)
                .name("کاتالوگ کامل")
                .imageKey("existing-key.png")
                .suggestedSellPrice(500000L)
                .build();
        DaryamartClient neverCalled = new DaryamartClient() {
            @Override
            public SepehrSearchResponseDto searchProducts(String key, int pageNumber, int pageSize) {
                throw new AssertionError("Daryamart must not be called for a complete catalog row");
            }
        };
        CatalogProductRepository repo = org.mockito.Mockito.mock(CatalogProductRepository.class);
        org.mockito.Mockito.when(repo.findByBarcodeAndIsActiveTrue("6261145000407"))
                .thenReturn(Optional.of(complete));

        BarcodeLookupServiceImpl service = serviceWithClients(
                neverCalled,
                new SehSootehClient() {
                    @Override
                    public SepehrSearchResponseDto searchProducts(String key, int pageNumber, int pageSize) {
                        throw new AssertionError("Seh Sooteh must not be called for a complete catalog row");
                    }
                },
                new TisaMarketClient() {
                    @Override
                    public SepehrSearchResponseDto searchProducts(String key, int pageNumber, int pageSize) {
                        throw new AssertionError("Tisa Market must not be called for a complete catalog row");
                    }
                },
                repo,
                imageStorageFailingWith("must not store anything"));

        Optional<BarcodeProductResponseDTO> result = service.lookupByBarcode("6261145000407");

        assertThat(result).isPresent();
        assertThat(result.get().getCatalogId()).isEqualTo(7L);
        assertThat(result.get().getSellPrice()).isEqualTo(500000L);
        // ImageUrlResolver with no configured base URL (unit test) -> relative /api/images URL
        assertThat(result.get().getImageUrl()).isEqualTo("/api/images/existing-key.png");
    }

    @Test
    void enrichesCatalogHitMissingPriceAndImageFromDaryamartAndStoresImageInMinio() {
        CatalogProduct incomplete = CatalogProduct.builder()
                .id(9L)
                .name("بیسکویت شکو چیپس شکلاتی سلامت")
                .build(); // no suggestedSellPrice, no imageKey
        CatalogProductRepository repo = org.mockito.Mockito.mock(CatalogProductRepository.class);
        org.mockito.Mockito.when(repo.findByBarcodeAndIsActiveTrue("6261145000407"))
                .thenReturn(Optional.of(incomplete));
        org.mockito.Mockito.when(repo.save(org.mockito.ArgumentMatchers.any(CatalogProduct.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        com.stockguard.service.ImageStorageService storage =
                org.mockito.Mockito.mock(com.stockguard.service.ImageStorageService.class);
        org.mockito.Mockito.when(storage.storeFromUrl(DARYAMART_BASE_URL + "/host/shabazi/2024/12/97863679310338.png"))
                .thenReturn("uuid-97863679310338.png");

        BarcodeLookupServiceImpl service = serviceWithClients(
                FakeClients.daryamart(successfulResponse),
                FakeClients.sehSooteh(null),
                FakeClients.tisaMarket(null),
                repo,
                storage);

        Optional<BarcodeProductResponseDTO> result = service.lookupByBarcode("6261145000407");

        assertThat(result).isPresent();
        assertThat(result.get().getCatalogId()).isEqualTo(9L);
        assertThat(result.get().getSellPrice()).isEqualTo(1329000L); // toman -> rial, now persisted
        assertThat(result.get().getImageUrl()).isEqualTo("/api/images/uuid-97863679310338.png");
        assertThat(incomplete.getSuggestedSellPrice()).isEqualTo(1329000L);
        assertThat(incomplete.getImageKey()).isEqualTo("uuid-97863679310338.png");
        org.mockito.Mockito.verify(storage)
                .storeFromUrl(DARYAMART_BASE_URL + "/host/shabazi/2024/12/97863679310338.png");
        org.mockito.Mockito.verify(repo).save(incomplete);
    }

    /**
     * Rows saved before MinIO ingestion hold the absolute Daryamart URL in
     * image_key. Such a URL is not an owned image: the lookup must re-fetch
     * from Daryamart, store the image in MinIO and replace the legacy URL.
     */
    @Test
    void replacesLegacyExternalImageUrlWithStoredMinioKey() {
        CatalogProduct legacy = CatalogProduct.builder()
                .id(11L)
                .name("شیرین عسل بیسکویت کرمدار کاکائو 120گ")
                .imageKey("https://daryamart.ir/host/shabazi/2026/2/96681783646756.jpg")
                .suggestedSellPrice(550000L)
                .build();
        CatalogProductRepository repo = org.mockito.Mockito.mock(CatalogProductRepository.class);
        org.mockito.Mockito.when(repo.findByBarcodeAndIsActiveTrue("6261149010686"))
                .thenReturn(Optional.of(legacy));
        org.mockito.Mockito.when(repo.save(org.mockito.ArgumentMatchers.any(CatalogProduct.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        com.stockguard.service.ImageStorageService storage =
                org.mockito.Mockito.mock(com.stockguard.service.ImageStorageService.class);
        org.mockito.Mockito.when(storage.storeFromUrl(DARYAMART_BASE_URL + "/host/shabazi/2024/12/97863679310338.png"))
                .thenReturn("uuid-97863679310338.png");

        BarcodeLookupServiceImpl service = serviceWithClients(
                FakeClients.daryamart(successfulResponse),
                FakeClients.sehSooteh(null),
                FakeClients.tisaMarket(null),
                repo,
                storage);

        Optional<BarcodeProductResponseDTO> result = service.lookupByBarcode("6261149010686");

        assertThat(result).isPresent();
        // price was already there and must stay untouched
        assertThat(result.get().getSellPrice()).isEqualTo(550000L);
        assertThat(result.get().getImageUrl()).isEqualTo("/api/images/uuid-97863679310338.png");
        assertThat(legacy.getImageKey()).isEqualTo("uuid-97863679310338.png");
        org.mockito.Mockito.verify(repo).save(legacy);
    }

    @Test
    void fallsBackToSehSootehWhenDaryamartMisses() throws Exception {
        // Seh Sooteh returns the same envelope with its own product id and image
        SepehrSearchResponseDto sehSootehResponse = objectMapper.readValue(RESPONSE_JSON
                        .replace("2413292", "2666389")
                        .replace("/host/shabazi/2024/12/97863679310338.png",
                                "/host/3soote/2025/12/99408152215729.jpg"),
                SepehrSearchResponseDto.class);

        CatalogProductRepository repo = emptyCatalogRepository();
        BarcodeLookupServiceImpl service = serviceWithClients(
                FakeClients.daryamart(emptyResponse()),
                FakeClients.sehSooteh(sehSootehResponse),
                FakeClients.tisaMarket(null),
                repo,
                imageStorageFailingWith("MinIO unavailable in unit test"));

        Optional<BarcodeProductResponseDTO> result = service.lookupByBarcode("6260265105894");

        assertThat(result).isPresent();
        assertThat(result.get().getName()).isEqualTo("بیسکویت شکو چیپس شکلاتی سلامت");
        // relative image resolved against the Seh Sooteh base URL, not Daryamart's
        assertThat(result.get().getImageUrl())
                .isEqualTo(SEHSOOTEH_BASE_URL + "/host/3soote/2025/12/99408152215729.jpg");
        // saved row is attributed to SEHSOOTEH
        org.mockito.Mockito.verify(repo).save(org.mockito.ArgumentMatchers.argThat(p ->
                "SEHSOOTEH".equals(p.getExternalSource())
                        && Long.valueOf(2666389L).equals(p.getExternalSourceId())));
    }

    @Test
    void skipsUnreachableShopAndTriesNextOne() {
        DaryamartClient failing = new DaryamartClient() {
            @Override
            public SepehrSearchResponseDto searchProducts(String key, int pageNumber, int pageSize) {
                throw new RuntimeException("connect timed out");
            }
        };

        BarcodeLookupServiceImpl service = serviceWithClients(
                failing,
                FakeClients.sehSooteh(successfulResponse),
                FakeClients.tisaMarket(null),
                emptyCatalogRepository(),
                imageStorageFailingWith("MinIO unavailable in unit test"));

        Optional<BarcodeProductResponseDTO> result = service.lookupByBarcode("6261145000407");

        assertThat(result).isPresent();
        assertThat(result.get().getImageUrl())
                .startsWith(SEHSOOTEH_BASE_URL);
    }

    @Test
    void returnsEmptyWhenAllShopsMiss() {
        BarcodeLookupServiceImpl service = serviceWithClients(
                FakeClients.daryamart(emptyResponse()),
                FakeClients.sehSooteh(emptyResponse()),
                FakeClients.tisaMarket(emptyResponse()),
                emptyCatalogRepository(),
                imageStorageFailingWith("MinIO unavailable in unit test"));

        assertThat(service.lookupByBarcode("0000000000000")).isEmpty();
    }

    @Test
    void fallsBackToSoocenterWhenAllSepehrShopsMiss() {
        // captured verbatim from 4soocenter.ir (mweb admin-ajax + product JSON-LD)
        String snippet = """
                <div class="ajax_search_list vertical_scroll_css">
                    <div class="post_with_thumb">
                        <div class="post-thumb"></div>
                        <div class="inner">
                            <a href="https://4soocenter.ir/product/%d9%85%d8%ad%d8%b5%d9%88%d9%84-4928/" title="شیرین عسل-شوکو کیک (وانیل)-40 گرمی" target="_blank">شیرین عسل-شوکو کیک (وانیل)-40 گرمی</a>
                        </div>
                    </div>
                </div>
                """;
        String productPage = """
                <script type="application/ld+json">{"@context":"https://schema.org/","@type":"Product",
                "name":"شیرین عسل-شوکو کیک (وانیل)-40 گرمی","description":null,"sku":"6261149137093","mpn":null,
                "offers":{"@type":"Offer","price":"185000","priceCurrency":"IRR",
                "url":"https://4soocenter.ir/product/%d9%85%d8%ad%d8%b5%d9%88%d9%84-4928/"}}</script>
                """;
        SoocenterSource soocenter = new SoocenterSource("https://4soocenter.ir") {
            @Override
            protected String searchSnippet(String barcode) {
                return snippet;
            }

            @Override
            protected String fetchProductPage(String productUrl) {
                return productPage;
            }
        };

        CatalogProductRepository repo = emptyCatalogRepository();
        BarcodeLookupServiceImpl service = serviceWithClients(
                FakeClients.daryamart(emptyResponse()),
                FakeClients.sehSooteh(emptyResponse()),
                FakeClients.tisaMarket(emptyResponse()),
                soocenter,
                repo,
                imageStorageFailingWith("MinIO unavailable in unit test"));

        Optional<BarcodeProductResponseDTO> result = service.lookupByBarcode("6261149137093");

        assertThat(result).isPresent();
        assertThat(result.get().getName()).isEqualTo("شیرین عسل-شوکو کیک (وانیل)-40 گرمی");
        assertThat(result.get().getSellPrice()).isEqualTo(185000L); // IRR as-is, no ×10
        org.mockito.Mockito.verify(repo).save(org.mockito.ArgumentMatchers.argThat(p ->
                "SOOCENTER".equals(p.getExternalSource())
                        && Long.valueOf(4928L).equals(p.getExternalSourceId())));
    }

    @Test
    void fallsBackToKowsarplazaWhenAllEarlierShopsMiss() {
        // captured verbatim from kowsarplaza.com (mweb admin-ajax + single-product page)
        String snippet = """
                <div class="ajax_search_list horizontal_scroll_css"><div class="post_with_thumb"><div class="post-thumb"><img width="300" height="300" src="http://kowsarplaza.com/wp-content/uploads/2021/11/1-5.gif" class="attachment-mini_blog size-mini_blog wp-post-image" alt="شوکو کیک" /></div><div class="inner"><a href="https://kowsarplaza.com/product/%d8%b4%d9%88%da%a9%d9%88-%da%a9%db%8c%da%a9-%d8%af%d9%88%d9%82%d9%84%d9%88-%d8%b1%d9%88%da%a9%d8%b4%d8%af%d8%a7%d8%b1-%d8%a2%d9%84%d8%a8%db%8c%d9%86%d8%a7-40%da%af%d8%b1%d9%85-%d8%b4%db%8c%d8%b1%db%8c/" title="شوکو کیک دوقلو روکشدار آلبینا 40گرم شیرین عسل 6261149137093" target="_blank">شوکو کیک دوقلو روکشدار آلبینا 40گرم شیرین عسل 6261149137093</a><span class="price"><del aria-hidden="true"><span class="woocommerce-Price-amount amount"><bdi>25.000<span class="woocommerce-Price-currencySymbol">تومان</span></bdi></span></del> <ins aria-hidden="true"><span class="woocommerce-Price-amount amount"><bdi>24.200<span class="woocommerce-Price-currencySymbol">تومان</span></bdi></span></ins></span></div></div></div>
                """;
        String productPage = """
                <html><head><title>شوکو کیک دوقلو روکشدار آلبینا 40گرم شیرین عسل 6261149137093 &#8211; کوثرپلازا</title></head>
                <body class="single single-product postid-27898 theme-mweb-digiland-pro woocommerce">
                <img src="https://kowsarplaza.com/wp-content/uploads/2021/11/1-5.gif" class="woocommerce-main-image" data-src="https://kowsarplaza.com/wp-content/uploads/2021/11/1-5.gif" />
                <p class="price single_price"><del><bdi>25.000<span>تومان</span></bdi></del> <ins><bdi>24.200<span>تومان</span></bdi></ins></p>
                </body></html>
                """;
        KowsarplazaSource kowsarplaza = new KowsarplazaSource("https://kowsarplaza.com") {
            @Override
            protected String searchSnippet(String barcode) {
                return snippet;
            }

            @Override
            protected String fetchProductPage(String productUrl) {
                return productPage;
            }
        };

        CatalogProductRepository repo = emptyCatalogRepository();
        BarcodeLookupServiceImpl service = serviceWithClients(
                FakeClients.daryamart(emptyResponse()),
                FakeClients.sehSooteh(emptyResponse()),
                FakeClients.tisaMarket(emptyResponse()),
                soocenterMissing(),
                kowsarplaza,
                repo,
                imageStorageFailingWith("MinIO unavailable in unit test"));

        Optional<BarcodeProductResponseDTO> result = service.lookupByBarcode("6261149137093");

        assertThat(result).isPresent();
        // trailing barcode stripped from the shop's title, Toman price ×10
        assertThat(result.get().getName()).isEqualTo("شوکو کیک دوقلو روکشدار آلبینا 40گرم شیرین عسل");
        assertThat(result.get().getSellPrice()).isEqualTo(242000L);
        org.mockito.Mockito.verify(repo).save(org.mockito.ArgumentMatchers.argThat(p ->
                "KOWSARPLAZA".equals(p.getExternalSource())
                        && Long.valueOf(27898L).equals(p.getExternalSourceId())));
    }
}
