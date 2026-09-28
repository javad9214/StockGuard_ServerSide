package com.stockguard.service;

import com.stockguard.data.dto.userproduct.request.UserProductDTO;
import com.stockguard.data.entity.CatalogProduct;
import com.stockguard.data.entity.UserProduct;
import com.stockguard.exception.ProductNotFoundException;
import com.stockguard.repository.CatalogProductRepository;
import com.stockguard.repository.SubcategoryRepository;
import com.stockguard.repository.UserProductRepository;
import com.stockguard.service.impl.UserProductServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Plain unit test for the adopt/link catalog semantics: adopt must be
 * idempotent (a retried push converges to the existing row) and carry the
 * form name, and updates may attach a catalog link with adopt's validation.
 */
@ExtendWith(MockitoExtension.class)
class UserProductServiceImplTest {

    private static final Long USER_ID = 7L;
    private static final Long CATALOG_ID = 100L;

    @Mock
    private UserProductRepository userProductRepository;

    @Mock
    private CatalogProductRepository catalogProductRepository;

    @Mock
    private SubcategoryRepository subcategoryRepository;

    private UserProductServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new UserProductServiceImpl(
                userProductRepository, catalogProductRepository, subcategoryRepository);
    }

    private CatalogProduct verifiedCatalog() {
        return CatalogProduct.builder()
                .id(CATALOG_ID)
                .barcode("12345678")
                .status(CatalogProduct.CatalogStatus.VERIFIED)
                .adoptionCount(3)
                .isActive(true)
                .build();
    }

    @Test
    void adoptSavesLinkedProductAndBumpsAdoptionCount() throws IOException {
        when(userProductRepository.findByUserIdAndCatalogProductIdAndIsDeletedFalse(USER_ID, CATALOG_ID))
                .thenReturn(Optional.empty());
        CatalogProduct catalog = verifiedCatalog();
        when(catalogProductRepository.findById(CATALOG_ID)).thenReturn(Optional.of(catalog));
        when(userProductRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        UserProductDTO dto = new UserProductDTO();
        dto.setCustomName("شیر پر چرب");
        dto.setPrice(1000L);
        dto.setCostPrice(800L);

        UserProduct adopted = service.adoptCatalogProduct(USER_ID, CATALOG_ID, dto, null);

        ArgumentCaptor<UserProduct> captor = ArgumentCaptor.forClass(UserProduct.class);
        verify(userProductRepository).save(captor.capture());
        UserProduct saved = captor.getValue();
        assertThat(saved.getCatalogProduct()).isSameAs(catalog);
        assertThat(saved.getCustomName()).isEqualTo("شیر پر چرب");
        assertThat(saved.getBarcode()).isEqualTo("12345678"); // fell back to the catalog barcode
        assertThat(saved.getSynced()).isTrue();
        assertThat(catalog.getAdoptionCount()).isEqualTo(4);
        verify(catalogProductRepository).save(catalog);
        assertThat(adopted).isSameAs(saved);
    }

    @Test
    void adoptIsIdempotentWhenAlreadyAdopted() throws IOException {
        UserProduct existing = new UserProduct();
        existing.setId(55L);
        when(userProductRepository.findByUserIdAndCatalogProductIdAndIsDeletedFalse(USER_ID, CATALOG_ID))
                .thenReturn(Optional.of(existing));

        UserProduct result = service.adoptCatalogProduct(USER_ID, CATALOG_ID, new UserProductDTO(), null);

        assertThat(result).isSameAs(existing);
        verify(userProductRepository, never()).save(any());
        verify(catalogProductRepository, never()).save(any());
    }

    @Test
    void adoptRejectsUnverifiedCatalogProduct() {
        when(userProductRepository.findByUserIdAndCatalogProductIdAndIsDeletedFalse(USER_ID, CATALOG_ID))
                .thenReturn(Optional.empty());
        CatalogProduct pending = verifiedCatalog();
        pending.setStatus(CatalogProduct.CatalogStatus.PENDING_REVIEW);
        when(catalogProductRepository.findById(CATALOG_ID)).thenReturn(Optional.of(pending));

        assertThatThrownBy(() ->
                service.adoptCatalogProduct(USER_ID, CATALOG_ID, new UserProductDTO(), null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Catalog product is not available");
    }

    @Test
    void updateLinksCustomProductToCatalog() throws IOException {
        UserProduct existing = new UserProduct();
        existing.setId(9L);
        when(userProductRepository.findByIdAndUserId(9L, USER_ID))
                .thenReturn(Optional.of(existing));
        when(userProductRepository.findByUserIdAndCatalogProductIdAndIsDeletedFalse(USER_ID, CATALOG_ID))
                .thenReturn(Optional.empty());
        CatalogProduct catalog = verifiedCatalog();
        when(catalogProductRepository.findById(CATALOG_ID)).thenReturn(Optional.of(catalog));
        when(userProductRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        UserProductDTO dto = new UserProductDTO();
        dto.setCatalogProductId(CATALOG_ID);
        dto.setCustomName("new name");

        service.updateUserProduct(USER_ID, 9L, dto, null);

        assertThat(existing.getCatalogProduct()).isSameAs(catalog);
        assertThat(catalog.getAdoptionCount()).isEqualTo(4);
        verify(catalogProductRepository).save(catalog);
    }

    @Test
    void updateTreatsMatchingLinkAsNoOp() throws IOException {
        UserProduct existing = new UserProduct();
        existing.setId(9L);
        existing.setCatalogProduct(verifiedCatalog());
        when(userProductRepository.findByIdAndUserId(9L, USER_ID))
                .thenReturn(Optional.of(existing));
        when(userProductRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        UserProductDTO dto = new UserProductDTO();
        dto.setCatalogProductId(CATALOG_ID);

        service.updateUserProduct(USER_ID, 9L, dto, null);

        verify(catalogProductRepository, never()).save(any());
    }

    @Test
    void updateRejectsLinkHeldByAnotherProduct() {
        UserProduct existing = new UserProduct();
        existing.setId(9L);
        when(userProductRepository.findByIdAndUserId(9L, USER_ID))
                .thenReturn(Optional.of(existing));
        UserProduct other = new UserProduct();
        other.setId(10L);
        when(userProductRepository.findByUserIdAndCatalogProductIdAndIsDeletedFalse(USER_ID, CATALOG_ID))
                .thenReturn(Optional.of(other));

        UserProductDTO dto = new UserProductDTO();
        dto.setCatalogProductId(CATALOG_ID);

        assertThatThrownBy(() ->
                service.updateUserProduct(USER_ID, 9L, dto, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Catalog product already adopted");
    }

    @Test
    void updateThrowsWhenProductMissing() {
        when(userProductRepository.findByIdAndUserId(9L, USER_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                service.updateUserProduct(USER_ID, 9L, new UserProductDTO(), null))
                .isInstanceOf(ProductNotFoundException.class);
    }
}
