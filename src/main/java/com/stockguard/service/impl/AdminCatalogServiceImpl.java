package com.stockguard.service.impl;

import com.stockguard.data.entity.CatalogProduct;
import com.stockguard.repository.CatalogProductRepository;
import com.stockguard.service.AdminCatalogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminCatalogServiceImpl implements AdminCatalogService {

    private final CatalogProductRepository catalogProductRepository;

    @Override
    @Transactional(readOnly = true)
    public Page<CatalogProduct> getAllProducts(Pageable pageable) {
        return catalogProductRepository.findAll(pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CatalogProduct> getByStatus(CatalogProduct.CatalogStatus status, Pageable pageable) {
        return catalogProductRepository.findByStatusAndIsActiveTrue(status, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CatalogProduct> getPendingReview(Pageable pageable) {
        return catalogProductRepository.findPendingReview(pageable);
    }

    @Override
    @Transactional
    public CatalogProduct createCatalogProduct(CatalogProduct product) {
        log.info("Admin creating catalog product: {}", product.getName());

        // Check barcode uniqueness
        if (product.getBarcode() != null &&
                catalogProductRepository.existsByBarcode(product.getBarcode())) {
            throw new IllegalArgumentException("Barcode already exists");
        }

        product.setStatus(CatalogProduct.CatalogStatus.VERIFIED);
        product.setIsActive(true);
        product.setQualityScore(80); // Default admin-created quality

        CatalogProduct saved = catalogProductRepository.save(product);
        log.info("Catalog product created with ID: {}", saved.getId());

        return saved;
    }

    @Override
    @Transactional
    public CatalogProduct updateCatalogProduct(Long id, CatalogProduct product) {
        log.info("Admin updating catalog product: {}", id);

        CatalogProduct existing = catalogProductRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Product not found"));

        // Update fields
        existing.setName(product.getName());
        existing.setBarcode(product.getBarcode());
        existing.setDescription(product.getDescription());
        existing.setBrand(product.getBrand());
        existing.setManufacturer(product.getManufacturer());
        existing.setCategory(product.getCategory());
        existing.setSubcategory(product.getSubcategory());
        existing.setImageUrl(product.getImageUrl());
        existing.setSuggestedPrice(product.getSuggestedPrice());
        existing.setUnit(product.getUnit());
        existing.setTags(product.getTags());

        CatalogProduct updated = catalogProductRepository.save(existing);
        log.info("Catalog product updated: {}", id);

        return updated;
    }

    @Override
    @Transactional
    public void deleteCatalogProduct(Long id) {
        log.info("Admin deleting catalog product: {}", id);

        CatalogProduct product = catalogProductRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Product not found"));

        product.setIsActive(false);
        catalogProductRepository.save(product);

        log.info("Catalog product soft-deleted: {}", id);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new HashMap<>();

        stats.put("total", catalogProductRepository.count());
        stats.put("verified", catalogProductRepository.countByStatus(CatalogProduct.CatalogStatus.VERIFIED));
        stats.put("pending", catalogProductRepository.countByStatus(CatalogProduct.CatalogStatus.PENDING_REVIEW));
        stats.put("rejected", catalogProductRepository.countByStatus(CatalogProduct.CatalogStatus.REJECTED));

        return stats;
    }
}