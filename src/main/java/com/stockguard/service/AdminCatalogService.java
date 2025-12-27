package com.stockguard.service;

import com.stockguard.data.entity.CatalogProduct;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Map;

public interface AdminCatalogService {

    Page<CatalogProduct> getAllProducts(Pageable pageable);

    Page<CatalogProduct> getByStatus(CatalogProduct.CatalogStatus status, Pageable pageable);

    Page<CatalogProduct> getPendingReview(Pageable pageable);

    CatalogProduct createCatalogProduct(CatalogProduct product);

    CatalogProduct updateCatalogProduct(Long id, CatalogProduct product);

    void deleteCatalogProduct(Long id);

    Map<String, Object> getStatistics();
}