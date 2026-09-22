package com.stockguard.controller;

import com.stockguard.data.dto.common.PagedResponse;
import com.stockguard.data.dto.common.ResponseDTO;
import com.stockguard.data.entity.CatalogProduct;
import com.stockguard.data.enums.ResponseCode;
import com.stockguard.service.AdminCatalogService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/catalog")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminCatalogController extends BaseController {

    private final AdminCatalogService adminCatalogService;

    /**
     * Get all catalog products (including pending)
     * GET /api/admin/catalog/products?status=PENDING_REVIEW
     */
    @GetMapping("/products")
    public PagedResponse<CatalogProduct> getAllCatalogProducts(
            @RequestParam(required = false) String status,
            @PageableDefault(size = 20) Pageable pageable) {

        Page<CatalogProduct> pageResult;

        if (status != null) {
            CatalogProduct.CatalogStatus catalogStatus =
                    CatalogProduct.CatalogStatus.valueOf(status.toUpperCase());
            pageResult = adminCatalogService.getByStatus(catalogStatus, pageable);
        } else {
            pageResult = adminCatalogService.getAllProducts(pageable);
        }

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
     * Create catalog product (admin only)
     * POST /api/admin/catalog/products
     */
    @PostMapping("/products")
    public ResponseEntity<ResponseDTO<Long>> createCatalogProduct(
            @Valid @RequestBody CatalogProduct product) {
        try {
            CatalogProduct created = adminCatalogService.createCatalogProduct(product);
            return generateOKResponse(created.getId());
        } catch (Exception e) {
            return generateErrorResponse(HttpStatus.BAD_REQUEST, ResponseCode.VALIDATION_ERROR, e.getMessage());
        }
    }

    /**
     * Update catalog product
     * PUT /api/admin/catalog/products/{id}
     */
    @PutMapping("/products/{id}")
    public ResponseEntity<ResponseDTO<Void>> updateCatalogProduct(
            @PathVariable Long id,
            @Valid @RequestBody CatalogProduct product) {
        try {
            adminCatalogService.updateCatalogProduct(id, product);
            return generateOKResponse(null);
        } catch (IllegalArgumentException e) {
            return generateErrorResponse(HttpStatus.NOT_FOUND, ResponseCode.NOT_FOUND, e.getMessage());
        }
    }

    /**
     * Delete catalog product
     * DELETE /api/admin/catalog/products/{id}
     */
    @DeleteMapping("/products/{id}")
    public ResponseEntity<ResponseDTO<Void>> deleteCatalogProduct(@PathVariable Long id) {
        try {
            adminCatalogService.deleteCatalogProduct(id);
            return generateOKResponse(null);
        } catch (IllegalArgumentException e) {
            return generateErrorResponse(HttpStatus.NOT_FOUND, ResponseCode.NOT_FOUND, e.getMessage());
        }
    }

    /**
     * Get pending review products
     * GET /api/admin/catalog/pending
     */
    @GetMapping("/pending")
    public PagedResponse<CatalogProduct> getPendingProducts(
            @PageableDefault(size = 20) Pageable pageable) {

        Page<CatalogProduct> pageResult = adminCatalogService.getPendingReview(pageable);

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
     * Get catalog statistics
     * GET /api/admin/catalog/statistics
     */
    @GetMapping("/statistics")
    public ResponseEntity<?> getCatalogStatistics() {
        return ResponseEntity.ok(adminCatalogService.getStatistics());
    }
}