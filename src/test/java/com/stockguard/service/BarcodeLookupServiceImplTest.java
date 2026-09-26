package com.stockguard.service;

import com.stockguard.client.DaryamartClient;
import com.stockguard.data.dto.barcode.response.BarcodeProductResponseDTO;
import com.stockguard.data.dto.daryamart.DaryamartSearchResponseDto;
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
 * Plain unit test: the Daryamart payload below was captured verbatim from the
 * live GetProductsSearch endpoint, so it also pins the JSON deserialization
 * (field names, isSuccess mapping, relative imageAddress).
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

    private static final String BASE_URL = "https://daryamart.ir";

    private final ObjectMapper objectMapper = new ObjectMapper();

    private DaryamartSearchResponseDto successfulResponse;
    private BarcodeLookupServiceImpl service;

    @BeforeEach
    void setUp() throws Exception {
        successfulResponse = objectMapper.readValue(RESPONSE_JSON, DaryamartSearchResponseDto.class);
    }

    private BarcodeLookupServiceImpl serviceReturning(DaryamartSearchResponseDto response) {
        return serviceReturning(response, imageStorageFailingWith("MinIO unavailable in unit test"));
    }

    private BarcodeLookupServiceImpl serviceReturning(
            DaryamartSearchResponseDto response,
            com.stockguard.service.ImageStorageService imageStorage) {
        DaryamartClient fakeClient = new DaryamartClient() {
            @Override
            public DaryamartSearchResponseDto searchProducts(String key, int pageNumber, int pageSize) {
                return response;
            }
        };
        BarcodeLookupServiceImpl impl = new BarcodeLookupServiceImpl(
                fakeClient,
                emptyCatalogRepository(),
                org.mockito.Mockito.mock(com.stockguard.repository.CategoryRepository.class),
                org.mockito.Mockito.mock(com.stockguard.repository.SubcategoryRepository.class),
                imageStorage);
        ReflectionTestUtils.setField(impl, "baseUrl", BASE_URL);
        return impl;
    }

    /**
     * Storage that never succeeds: storeImage then falls back to the direct
     * Daryamart URL, which is what the pre-MinIO assertions pin.
     */
    private com.stockguard.service.ImageStorageService imageStorageFailingWith(String reason) {
        com.stockguard.service.ImageStorageService storage =
                org.mockito.Mockito.mock(com.stockguard.service.ImageStorageService.class);
        org.mockito.Mockito.when(storage.storeFromUrl(org.mockito.ArgumentMatchers.anyString()))
                .thenThrow(new RuntimeException(reason));
        return storage;
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

    @Test
    void mapsFirstProductWithNameSellPriceAndAbsoluteImageUrl() {
        service = serviceReturning(successfulResponse);

        Optional<BarcodeProductResponseDTO> result = service.lookupByBarcode("6261145000407");

        assertThat(result).isPresent();
        assertThat(result.get().getName()).isEqualTo("بیسکویت شکو چیپس شکلاتی سلامت");
        assertThat(result.get().getSellPrice()).isEqualTo(1329000L); // toman 132900 converted to rial
        assertThat(result.get().getImageUrl())
                .isEqualTo(BASE_URL + "/host/shabazi/2024/12/97863679310338.png");
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
        service = serviceReturning(successfulResponse);

        assertThat(service.lookupByBarcode("6261145000407")).isEmpty();
    }

    @Test
    void returnsEmptyWhenNoProductMatches() {
        successfulResponse.getData().setSource(java.util.List.of());
        service = serviceReturning(successfulResponse);

        assertThat(service.lookupByBarcode("0000000000000")).isEmpty();
    }

    @Test
    void returnsNullImageUrlWhenAddressIsBlank() throws Exception {
        successfulResponse.getData().getSource().get(0).setImageAddress("   ");
        service = serviceReturning(successfulResponse);

        Optional<BarcodeProductResponseDTO> result = service.lookupByBarcode("6261145000407");

        assertThat(result).isPresent();
        assertThat(result.get().getImageUrl()).isNull();
    }

    @Test
    void keepsAbsoluteImageUrlAsIs() {
        successfulResponse.getData().getSource().get(0).setImageAddress("https://cdn.example.com/img.png");
        service = serviceReturning(successfulResponse);

        assertThat(service.lookupByBarcode("6261145000407"))
                .map(BarcodeProductResponseDTO::getImageUrl)
                .contains("https://cdn.example.com/img.png");
    }

    @Test
    void returnsCompleteCatalogHitWithoutCallingDaryamart() {
        CatalogProduct complete = CatalogProduct.builder()
                .id(7L)
                .name("کاتالوگ کامل")
                .imageKey("existing-key.png")
                .suggestedSellPrice(500000L)
                .build();
        DaryamartClient neverCalled = new DaryamartClient() {
            @Override
            public DaryamartSearchResponseDto searchProducts(String key, int pageNumber, int pageSize) {
                throw new AssertionError("Daryamart must not be called for a complete catalog row");
            }
        };
        CatalogProductRepository repo = org.mockito.Mockito.mock(CatalogProductRepository.class);
        org.mockito.Mockito.when(repo.findByBarcodeAndIsActiveTrue("6261145000407"))
                .thenReturn(Optional.of(complete));

        service = new BarcodeLookupServiceImpl(
                neverCalled,
                repo,
                org.mockito.Mockito.mock(com.stockguard.repository.CategoryRepository.class),
                org.mockito.Mockito.mock(com.stockguard.repository.SubcategoryRepository.class),
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
        org.mockito.Mockito.when(storage.storeFromUrl(BASE_URL + "/host/shabazi/2024/12/97863679310338.png"))
                .thenReturn("uuid-97863679310338.png");

        service = new BarcodeLookupServiceImpl(
                new DaryamartClient() {
                    @Override
                    public DaryamartSearchResponseDto searchProducts(String key, int pageNumber, int pageSize) {
                        return successfulResponse;
                    }
                },
                repo,
                org.mockito.Mockito.mock(com.stockguard.repository.CategoryRepository.class),
                org.mockito.Mockito.mock(com.stockguard.repository.SubcategoryRepository.class),
                storage);
        ReflectionTestUtils.setField(service, "baseUrl", BASE_URL);

        Optional<BarcodeProductResponseDTO> result = service.lookupByBarcode("6261145000407");

        assertThat(result).isPresent();
        assertThat(result.get().getCatalogId()).isEqualTo(9L);
        assertThat(result.get().getSellPrice()).isEqualTo(1329000L); // toman -> rial, now persisted
        assertThat(result.get().getImageUrl()).isEqualTo("/api/images/uuid-97863679310338.png");
        assertThat(incomplete.getSuggestedSellPrice()).isEqualTo(1329000L);
        assertThat(incomplete.getImageKey()).isEqualTo("uuid-97863679310338.png");
        org.mockito.Mockito.verify(storage)
                .storeFromUrl(BASE_URL + "/host/shabazi/2024/12/97863679310338.png");
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
        org.mockito.Mockito.when(storage.storeFromUrl(BASE_URL + "/host/shabazi/2024/12/97863679310338.png"))
                .thenReturn("uuid-97863679310338.png");

        service = new BarcodeLookupServiceImpl(
                new DaryamartClient() {
                    @Override
                    public DaryamartSearchResponseDto searchProducts(String key, int pageNumber, int pageSize) {
                        return successfulResponse;
                    }
                },
                repo,
                org.mockito.Mockito.mock(com.stockguard.repository.CategoryRepository.class),
                org.mockito.Mockito.mock(com.stockguard.repository.SubcategoryRepository.class),
                storage);
        ReflectionTestUtils.setField(service, "baseUrl", BASE_URL);

        Optional<BarcodeProductResponseDTO> result = service.lookupByBarcode("6261149010686");

        assertThat(result).isPresent();
        // price was already there and must stay untouched
        assertThat(result.get().getSellPrice()).isEqualTo(550000L);
        assertThat(result.get().getImageUrl()).isEqualTo("/api/images/uuid-97863679310338.png");
        assertThat(legacy.getImageKey()).isEqualTo("uuid-97863679310338.png");
        org.mockito.Mockito.verify(repo).save(legacy);
    }
}
