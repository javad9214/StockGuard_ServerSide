package com.stockguard.service;

import com.stockguard.data.entity.CatalogProduct;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

public interface CatalogProductService {

    Page<CatalogProduct> getVerifiedProducts(Pageable pageable);

    Page<CatalogProduct> searchCatalog(String query, Pageable pageable);

    Optional<CatalogProduct> getCatalogProductById(Long id);

    Optional<CatalogProduct> getByBarcode(String barcode);

    Page<CatalogProduct> getByCategory(String category, Pageable pageable);

    Page<CatalogProduct> getTopAdopted(Pageable pageable);
}