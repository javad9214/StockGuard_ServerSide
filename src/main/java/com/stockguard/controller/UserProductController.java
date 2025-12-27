package com.stockguard.controller;

import com.stockguard.data.dto.ApiResponse;
import com.stockguard.data.dto.PagedResponse;
import com.stockguard.data.entity.UserProduct;
import com.stockguard.service.UserProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class UserProductController {

    private final UserProductService userProductService;

    /**
     * Get user's products
     * GET /api/products
     */
    @GetMapping
    public PagedResponse<UserProduct> getUserProducts(@PageableDefault(size = 20) Pageable pageable) {
        Long userId = getCurrentUserId();
        Page<UserProduct> pageResult = userProductService.getUserProducts(userId, pageable);

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
     * Get product by ID
     * GET /api/products/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<UserProduct> getProductById(@PathVariable Long id) {
        Long userId = getCurrentUserId();
        return userProductService.getUserProductById(userId, id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Create custom product
     * POST /api/products
     */
    @PostMapping
    public ResponseEntity<ApiResponse<Long>> createCustomProduct(@Valid @RequestBody UserProduct product) {
        try {
            Long userId = getCurrentUserId();
            UserProduct saved = userProductService.createCustomProduct(userId, product);

            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.success("Product created successfully", saved.getId()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Product creation failed", e.getMessage()));
        }
    }

    /**
     * Adopt catalog product
     * POST /api/products/adopt/{catalogProductId}
     */
    @PostMapping("/adopt/{catalogProductId}")
    public ResponseEntity<ApiResponse<Long>> adoptCatalogProduct(
            @PathVariable Long catalogProductId,
            @Valid @RequestBody UserProduct productData) {
        try {
            Long userId = getCurrentUserId();
            UserProduct adopted = userProductService.adoptCatalogProduct(userId, catalogProductId, productData);

            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.success("Product adopted successfully", adopted.getId()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error("Adoption failed", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Adoption failed", e.getMessage()));
        }
    }

    /**
     * Update product
     * PUT /api/products/{id}
     */
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> updateProduct(
            @PathVariable Long id,
            @Valid @RequestBody UserProduct product) {
        try {
            Long userId = getCurrentUserId();
            userProductService.updateUserProduct(userId, id, product);

            return ResponseEntity.ok(ApiResponse.success("Product updated successfully"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("Update failed", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Update failed", e.getMessage()));
        }
    }

    /**
     * Delete product (soft delete)
     * DELETE /api/products/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteProduct(@PathVariable Long id) {
        try {
            Long userId = getCurrentUserId();
            userProductService.deleteUserProduct(userId, id);

            return ResponseEntity.ok(ApiResponse.success("Product deleted successfully"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("Delete failed", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Delete failed", e.getMessage()));
        }
    }

    /**
     * Search user's products
     * GET /api/products/search?query=coca
     */
    @GetMapping("/search")
    public PagedResponse<UserProduct> searchProducts(
            @RequestParam String query,
            @PageableDefault(size = 20) Pageable pageable) {
        Long userId = getCurrentUserId();
        Page<UserProduct> pageResult = userProductService.searchUserProducts(userId, query, pageable);

        return new PagedResponse<>(
                pageResult.getContent(),
                pageResult.getNumber(),
                pageResult.getSize(),
                pageResult.getTotalElements(),
                pageResult.getTotalPages(),
                pageResult.isLast()
        );
    }

    private Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication != null && authentication.getPrincipal() instanceof Long) {
            return (Long) authentication.getPrincipal();
        }

        throw new IllegalStateException("User not authenticated");
    }
}