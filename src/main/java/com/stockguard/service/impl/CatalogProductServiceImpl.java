package com.stockguard.service.impl;

import com.stockguard.data.entity.CatalogProduct;
import com.stockguard.repository.CatalogProductRepository;
import com.stockguard.service.CatalogProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class CatalogProductServiceImpl implements CatalogProductService {

    private final CatalogProductRepository catalogProductRepository;

    @Override
    @Transactional(readOnly = true)
    public Page<CatalogProduct> getVerifiedProducts(Pageable pageable) {
        return catalogProductRepository.findByStatusAndIsActiveTrue(
                CatalogProduct.CatalogStatus.VERIFIED,
                pageable
        );
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CatalogProduct> searchCatalog(String query, Pageable pageable) {
        log.info("Searching catalog with query: {}", query);
        return catalogProductRepository.searchCatalog(query, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<CatalogProduct> getCatalogProductById(Long id) {
        return catalogProductRepository.findById(id)
                .filter(CatalogProduct::getIsActive);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<CatalogProduct> getByBarcode(String barcode) {
        return catalogProductRepository.findByBarcodeAndIsActiveTrue(barcode);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CatalogProduct> getByCategory(String category, Pageable pageable) {
        return catalogProductRepository.findByCategoryAndStatusAndIsActiveTrue(
                category,
                CatalogProduct.CatalogStatus.VERIFIED,
                pageable
        );
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CatalogProduct> getTopAdopted(Pageable pageable) {
        return catalogProductRepository.findTopAdopted(pageable);
    }
}