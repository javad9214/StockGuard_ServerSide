package com.stockguard.controller;

import com.stockguard.data.dto.common.PagedResponse;
import com.stockguard.data.dto.common.ResponseDTO;
import com.stockguard.data.dto.userproduct.request.UserProductDTO;
import com.stockguard.data.dto.userproduct.response.UserProductResponseDTO;
import com.stockguard.data.entity.UserProduct;
import com.stockguard.data.enums.ResponseCode;
import com.stockguard.exception.ProductNotFoundException;
import com.stockguard.service.UserProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class UserProductController extends BaseController {

    private final UserProductService userProductService;

    /**
     * Get user's products
     * GET /api/products
     */
    @GetMapping
    public PagedResponse<UserProductResponseDTO> getUserProducts(@PageableDefault(size = 20) Pageable pageable) {
        Long userId = getCurrentUserId();
        Page<UserProductResponseDTO> page = userProductService.getUserProducts(userId, pageable);

        return new PagedResponse<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isLast()
        );
    }

    /**
     * Get product by ID
     * GET /api/products/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<UserProductResponseDTO> getProductById(@PathVariable Long id) {
        Long userId = getCurrentUserId();
        return userProductService.getUserProductById(userId, id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Create custom product
     * POST /api/products
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ResponseDTO<Long>> createCustomProduct(
            @Valid @RequestPart("product") UserProductDTO product,
            @RequestPart(value = "image", required = false) MultipartFile image) {
        try {
            Long userId = getCurrentUserId();
            UserProduct saved = userProductService.createCustomProduct(userId, product, image);

            return generateOKResponse(saved.getId());
        } catch (IllegalArgumentException e) {
            return generateErrorResponse(HttpStatus.BAD_REQUEST, ResponseCode.VALIDATION_ERROR, e.getMessage());
        } catch (Exception e) {
            return generateErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, ResponseCode.INTERNAL_ERROR, e.getMessage());
        }
    }

    @PostMapping(value = "/adopt/{catalogProductId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ResponseDTO<Long>> adoptCatalogProduct(
            @PathVariable Long catalogProductId,
            @Valid @RequestPart("product") UserProductDTO productData,
            @RequestPart(value = "image", required = false) MultipartFile image) {
        try {
            Long userId = getCurrentUserId();
            UserProduct adopted = userProductService.adoptCatalogProduct(userId, catalogProductId, productData, image);

            return generateOKResponse(adopted.getId());
        } catch (IllegalArgumentException e) {
            return generateErrorResponse(HttpStatus.BAD_REQUEST, ResponseCode.VALIDATION_ERROR, e.getMessage());
        } catch (Exception e) {
            return generateErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, ResponseCode.INTERNAL_ERROR, e.getMessage());
        }
    }

    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ResponseDTO<Void>> updateProduct(
            @PathVariable Long id,
            @Valid @RequestPart("product") UserProductDTO product,
            @RequestPart(value = "image", required = false) MultipartFile image) {
        try {
            Long userId = getCurrentUserId();
            userProductService.updateUserProduct(userId, id, product, image);

            return generateOKResponse(null);
        } catch (ProductNotFoundException e) {
            return generateErrorResponse(HttpStatus.NOT_FOUND, ResponseCode.NOT_FOUND, e.getMessage());
        } catch (IllegalArgumentException e) {
            return generateErrorResponse(HttpStatus.BAD_REQUEST, ResponseCode.VALIDATION_ERROR, e.getMessage());
        } catch (Exception e) {
            return generateErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, ResponseCode.INTERNAL_ERROR, e.getMessage());
        }
    }

    /**
     * Delete product (soft delete)
     * DELETE /api/products/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ResponseDTO<Void>> deleteProduct(@PathVariable Long id) {
        try {
            Long userId = getCurrentUserId();
            userProductService.deleteUserProduct(userId, id);

            return generateOKResponse(null);
        } catch (ProductNotFoundException e) {
            return generateErrorResponse(HttpStatus.NOT_FOUND, ResponseCode.NOT_FOUND, e.getMessage());
        } catch (IllegalArgumentException e) {
            return generateErrorResponse(HttpStatus.BAD_REQUEST, ResponseCode.VALIDATION_ERROR, e.getMessage());
        } catch (Exception e) {
            return generateErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, ResponseCode.INTERNAL_ERROR, e.getMessage());
        }
    }

    /**
     * Search user's products
     * GET /api/products/search?query=coca
     */
    @GetMapping("/search")
    public PagedResponse<UserProductResponseDTO> searchProducts(
            @RequestParam String query,
            @PageableDefault(size = 20) Pageable pageable) {
        Long userId = getCurrentUserId();
        Page<UserProductResponseDTO> page = userProductService.searchUserProducts(userId, query, pageable);

        return new PagedResponse<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isLast()
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