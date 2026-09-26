package com.stockguard.controller;

import com.stockguard.data.dto.common.PagedResponse;
import com.stockguard.data.dto.common.ResponseDTO;
import com.stockguard.data.dto.image.response.ImageUploadResponseDTO;
import com.stockguard.data.entity.CatalogProduct;
import com.stockguard.data.enums.ResponseCode;
import com.stockguard.service.AdminCatalogService;
import com.stockguard.service.ImageStorageService;
import com.stockguard.util.ImageUrlResolver;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Slf4j
@RestController
@RequestMapping("/api/admin/catalog")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminCatalogController extends BaseController {

    private final AdminCatalogService adminCatalogService;
    private final ImageStorageService imageStorageService;

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
     * Upload an image and attach it to a catalog product in one call.
     * POST /api/admin/catalog/products/{id}/image (multipart field "file")
     *
     * Stores the image in MinIO and persists only the object key in
     * CatalogProduct.imageKey (never a URL). The response carries the
     * absolute URL built from app.base-url for immediate display. A
     * previously stored key is deleted best-effort; legacy external URLs
     * are left untouched (not owned by this service).
     */
    @PostMapping("/products/{id}/image")
    public ResponseEntity<ResponseDTO<ImageUploadResponseDTO>> uploadProductImage(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file) {
        try {
            CatalogProduct product = adminCatalogService.getProductById(id);
            String previousKey = product.getImageKey();

            String key = imageStorageService.upload(file);
            adminCatalogService.updateProductImageKey(id, key);

            if (ImageUrlResolver.isStoredKey(previousKey)) {
                deleteQuietly(previousKey);
            }
            return generateOKResponse(new ImageUploadResponseDTO(key, ImageUrlResolver.resolve(key)));
        } catch (IllegalArgumentException e) {
            return generateErrorResponse(HttpStatus.BAD_REQUEST, ResponseCode.VALIDATION_ERROR, e.getMessage());
        } catch (IOException e) {
            log.error("Failed to store image for catalog product {}", id, e);
            return generateErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, ResponseCode.INTERNAL_ERROR, "Failed to store image");
        }
    }

    private void deleteQuietly(String key) {
        try {
            imageStorageService.delete(key);
        } catch (Exception e) {
            // an orphaned object in the bucket is acceptable; don't fail the update
            log.warn("Failed to delete previous stored image '{}'", key, e);
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