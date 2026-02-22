package com.stockguard.controller;

import com.stockguard.data.dto.CatalogProductResponse;
import com.stockguard.data.dto.PagedResponse;
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

    @GetMapping("/products")
    public PagedResponse<CatalogProductResponse> browseCatalog(
            @PageableDefault(size = 20) Pageable pageable) {

        Page<CatalogProductResponse> page = catalogProductService
                .getVerifiedProducts(pageable)
                .map(CatalogProductResponse::from);

        return toPagedResponse(page);
    }

    @GetMapping("/products/search")
    public PagedResponse<CatalogProductResponse> searchCatalog(
            @RequestParam(name = "query", required = false, defaultValue = "") String query,
            @PageableDefault(size = 20) Pageable pageable) {

        if (query.isBlank()) {
            return toPagedResponse(catalogProductService.getVerifiedProducts(pageable)
                    .map(CatalogProductResponse::from));
        }

        return toPagedResponse(catalogProductService.searchCatalog(query, pageable)
                .map(CatalogProductResponse::from));
    }


    @GetMapping("/products/{id}")
    public ResponseEntity<CatalogProductResponse> getCatalogProduct(@PathVariable Long id) {
        return catalogProductService.getCatalogProductById(id)
                .map(CatalogProductResponse::from)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/products/category/{category}")
    public PagedResponse<CatalogProductResponse> getByCategory(
            @PathVariable String category,
            @PageableDefault(size = 20) Pageable pageable) {

        Page<CatalogProductResponse> page = catalogProductService
                .getByCategory(category, pageable)
                .map(CatalogProductResponse::from);

        return toPagedResponse(page);
    }

    @GetMapping("/products/popular")
    public PagedResponse<CatalogProductResponse> getPopularProducts(
            @PageableDefault(size = 20) Pageable pageable) {

        Page<CatalogProductResponse> page = catalogProductService
                .getTopAdopted(pageable)
                .map(CatalogProductResponse::from);

        return toPagedResponse(page);
    }

    @GetMapping("/products/barcode/{barcode}")
    public ResponseEntity<CatalogProductResponse> getByBarcode(@PathVariable String barcode) {
        return catalogProductService.getByBarcode(barcode)
                .map(CatalogProductResponse::from)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // ── Helper ──────────────────────────────────────────────
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
