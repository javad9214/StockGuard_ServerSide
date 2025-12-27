package com.stockguard.controller;

import com.stockguard.data.dto.PagedResponse;
import com.stockguard.data.entity.CatalogProduct;
import com.stockguard.service.CatalogProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/catalog")
@RequiredArgsConstructor
public class CatalogProductController {

    private final CatalogProductService catalogProductService;

    /**
     * Browse all verified catalog products
     * GET /api/catalog/products
     */
    @GetMapping("/products")
    public PagedResponse<CatalogProduct> browseCatalog(
            @PageableDefault(size = 20) Pageable pageable) {

        Page<CatalogProduct> pageResult = catalogProductService.getVerifiedProducts(pageable);

        return new PagedResponse<>(
                pageResult.getContent(),
                pageResult.getNumber(),
                pageResult.getSize(),
                pageResult.getTotalElements(),
                pageResult.getTotalPages(),
                pageResult.isLast()
        );
    }

    /**
     * Search catalog products
     * GET /api/catalog/products/search?q=coca
     */
    @GetMapping("/products/search")
    public PagedResponse<CatalogProduct> searchCatalog(
            @RequestParam String q,
            @PageableDefault(size = 20) Pageable pageable) {

        Page<CatalogProduct> pageResult = catalogProductService.searchCatalog(q, pageable);

        return new PagedResponse<>(
                pageResult.getContent(),
                pageResult.getNumber(),
                pageResult.getSize(),
                pageResult.getTotalElements(),
                pageResult.getTotalPages(),
                pageResult.isLast()
        );
    }

    /**
     * Get catalog product by ID
     * GET /api/catalog/products/{id}
     */
    @GetMapping("/products/{id}")
    public ResponseEntity<CatalogProduct> getCatalogProduct(@PathVariable Long id) {
        return catalogProductService.getCatalogProductById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Get catalog products by category
     * GET /api/catalog/products/category/{category}
     */
    @GetMapping("/products/category/{category}")
    public PagedResponse<CatalogProduct> getByCategory(
            @PathVariable String category,
            @PageableDefault(size = 20) Pageable pageable) {

        Page<CatalogProduct> pageResult = catalogProductService.getByCategory(category, pageable);

        return new PagedResponse<>(
                pageResult.getContent(),
                pageResult.getNumber(),
                pageResult.getSize(),
                pageResult.getTotalElements(),
                pageResult.getTotalPages(),
                pageResult.isLast()
        );
    }

    /**
     * Get top adopted products
     * GET /api/catalog/products/popular
     */
    @GetMapping("/products/popular")
    public PagedResponse<CatalogProduct> getPopularProducts(
            @PageableDefault(size = 20) Pageable pageable) {

        Page<CatalogProduct> pageResult = catalogProductService.getTopAdopted(pageable);

        return new PagedResponse<>(
                pageResult.getContent(),
                pageResult.getNumber(),
                pageResult.getSize(),
                pageResult.getTotalElements(),
                pageResult.getTotalPages(),
                pageResult.isLast()
        );
    }

    /**
     * Search by barcode
     * GET /api/catalog/products/barcode/{barcode}
     */
    @GetMapping("/products/barcode/{barcode}")
    public ResponseEntity<CatalogProduct> getByBarcode(@PathVariable String barcode) {
        return catalogProductService.getByBarcode(barcode)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}