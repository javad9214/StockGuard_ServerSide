package com.stockguard.controller;

import com.stockguard.data.dto.CatalogProductResponse;
import com.stockguard.data.dto.PagedResponse;
import com.stockguard.service.CatalogProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/catalog")
@RequiredArgsConstructor
public class CatalogProductController {

    private final CatalogProductService catalogProductService;

    @GetMapping("/products")
    public PagedResponse<CatalogProductResponse> browseCatalog(
            @PageableDefault(size = 20) Pageable pageable) {

        log.info("📋 Browsing catalog - page: {}, size: {}", pageable.getPageNumber(), pageable.getPageSize());

        Page<CatalogProductResponse> page = catalogProductService
                .getVerifiedProducts(pageable)
                .map(CatalogProductResponse::from);

        log.info("✅ Retrieved {} products (total: {})", page.getNumberOfElements(), page.getTotalElements());
        return toPagedResponse(page);
    }

    @GetMapping("/products/search")
    public PagedResponse<CatalogProductResponse> searchCatalog(
            @RequestParam(name = "query", required = false, defaultValue = "") String query,
            @PageableDefault(size = 20) Pageable pageable) {

        if (query.isBlank()) {
            log.info("🔍 Search with empty query, returning all verified products");
            return toPagedResponse(catalogProductService.getVerifiedProducts(pageable)
                    .map(CatalogProductResponse::from));
        }

        log.info("🔍 Searching catalog: '{}' - page: {}", query, pageable.getPageNumber());
        Page<CatalogProductResponse> results = catalogProductService.searchCatalog(query, pageable)
                .map(CatalogProductResponse::from);

        log.info("✅ Found {} results for '{}'", results.getTotalElements(), query);
        return toPagedResponse(results);
    }

    @GetMapping("/products/{id}")
    public ResponseEntity<CatalogProductResponse> getCatalogProduct(@PathVariable Long id) {
        log.info("🔎 Fetching product by ID: {}", id);

        return catalogProductService.getCatalogProductById(id)
                .map(CatalogProductResponse::from)
                .map(product -> {
                    log.info("✅ Product found: {} ({})", product.getName(), id);
                    return ResponseEntity.ok(product);
                })
                .orElseGet(() -> {
                    log.warn("⚠️ Product not found: {}", id);
                    return ResponseEntity.notFound().build();
                });
    }

    @GetMapping("/products/category/{category}")
    public PagedResponse<CatalogProductResponse> getByCategory(
            @PathVariable String category,
            @PageableDefault(size = 20) Pageable pageable) {

        log.info("📂 Fetching products by category: '{}'", category);

        Page<CatalogProductResponse> page = catalogProductService
                .getByCategory(category, pageable)
                .map(CatalogProductResponse::from);

        log.info("✅ Found {} products in category '{}'", page.getTotalElements(), category);
        return toPagedResponse(page);
    }

    @GetMapping("/products/popular")
    public PagedResponse<CatalogProductResponse> getPopularProducts(
            @PageableDefault(size = 20) Pageable pageable) {

        log.info("⭐ Fetching popular products");

        Page<CatalogProductResponse> page = catalogProductService
                .getTopAdopted(pageable)
                .map(CatalogProductResponse::from);

        log.info("✅ Retrieved {} popular products", page.getNumberOfElements());
        return toPagedResponse(page);
    }

    @GetMapping("/products/barcode/{barcode}")
    public ResponseEntity<CatalogProductResponse> getByBarcode(@PathVariable String barcode) {
        log.info("🏷️ Searching by barcode: {}", barcode);

        return catalogProductService.getByBarcode(barcode)
                .map(CatalogProductResponse::from)
                .map(product -> {
                    log.info("✅ Product found by barcode: {}", product.getName());
                    return ResponseEntity.ok(product);
                })
                .orElseGet(() -> {
                    log.warn("⚠️ No product found for barcode: {}", barcode);
                    return ResponseEntity.notFound().build();
                });
    }

    private <T> PagedResponse<T> toPagedResponse(Page<T> page) {
        return new PagedResponse<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isLast()
        );
    }
}
