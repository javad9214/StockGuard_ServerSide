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
                org.mockito.Mockito.mock(com.stockguard.repository.SubcategoryRepository.class));
        ReflectionTestUtils.setField(impl, "baseUrl", BASE_URL);
        return impl;
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
}
