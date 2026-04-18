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
        log.debug("Querying verified products - page: {}, size: {}",
                pageable.getPageNumber(), pageable.getPageSize());
        return catalogProductRepository.findByStatusAndIsActiveTrueWithJoins(
                CatalogProduct.CatalogStatus.VERIFIED,
                pageable
        );
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CatalogProduct> searchCatalog(String query, Pageable pageable) {
        log.debug("Executing catalog search: query='{}', page={}", query, pageable.getPageNumber());
        Page<CatalogProduct> results = catalogProductRepository.searchCatalog(query, pageable);
        log.debug("Search completed: {} results found", results.getTotalElements());
        return results;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<CatalogProduct> getCatalogProductById(Long id) {
        log.debug("Fetching catalog product by ID: {}", id);
        return catalogProductRepository.findById(id)
                .filter(CatalogProduct::getIsActive);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<CatalogProduct> getByBarcode(String barcode) {
        log.debug("Searching product by barcode: {}", barcode);
        return catalogProductRepository.findByBarcodeAndIsActiveTrue(barcode);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CatalogProduct> getByCategory(String category, Pageable pageable) {
        log.debug("Fetching products by category: {}", category);
        return catalogProductRepository.findBySubcategory_Category_NameAndStatusAndIsActiveTrue(
                category,
                CatalogProduct.CatalogStatus.VERIFIED,
                pageable
        );
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CatalogProduct> getTopAdopted(Pageable pageable) {
        log.debug("Fetching top adopted products");
        return catalogProductRepository.findTopAdopted(pageable);
    }
}
