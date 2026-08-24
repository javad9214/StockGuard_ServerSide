package com.stockguard.service.impl;

import com.stockguard.data.dto.UserProductDTO;
import com.stockguard.data.dto.UserProductResponseDTO;
import com.stockguard.data.entity.CatalogProduct;
import com.stockguard.data.entity.Subcategory;
import com.stockguard.data.entity.UserProduct;
import com.stockguard.repository.CatalogProductRepository;
import com.stockguard.repository.SubcategoryRepository;
import com.stockguard.repository.UserProductRepository;
import com.stockguard.service.UserProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserProductServiceImpl implements UserProductService {

    private final UserProductRepository userProductRepository;
    private final CatalogProductRepository catalogProductRepository;
    private final SubcategoryRepository subcategoryRepository;

    @Override
    @Transactional(readOnly = true)
    public Page<UserProductResponseDTO> getUserProducts(Long userId, Pageable pageable) {
        Page<UserProduct> products = userProductRepository.findByUserIdAndIsDeletedFalse(userId, pageable);
        Map<Integer, Subcategory> subcategoriesById = findSubcategoriesWithCategory(products.getContent());
        return products.map(p -> toResponse(p, subcategoriesById));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<UserProductResponseDTO> getUserProductById(Long userId, Long productId) {
        return userProductRepository.findByIdAndUserId(productId, userId)
                .map(p -> toResponse(p, findSubcategoriesWithCategory(List.of(p))));
    }

    @Override
    @Transactional
    public UserProduct createCustomProduct(Long userId, UserProductDTO dto, MultipartFile image) throws IOException {
        log.info("Creating custom product for user: {}", userId);

        UserProduct product = new UserProduct();
        product.setUserId(userId);
        product.setCatalogProduct(null); // Custom product
        product.setBarcode(dto.getBarcode());
        product.setCustomName(dto.getCustomName());
        product.setPrice(dto.getPrice());
        product.setCostPrice(dto.getCostPrice());
        product.setDescription(dto.getDescription());
        product.setSubcategoryId(dto.getSubcategoryId());
        product.setSupplierId(dto.getSupplierId());
        product.setUnit(dto.getUnit());
        product.setStock(dto.getStock() != null ? dto.getStock() : 0);
        product.setMinStockLevel(dto.getMinStockLevel());
        product.setMaxStockLevel(dto.getMaxStockLevel());
        product.setIsActive(dto.getIsActive() != null ? dto.getIsActive() : true);
        product.setTags(dto.getTags());
        product.setIsDeleted(false);
        // Persisted server-side → confirmed write
        product.setSynced(true);

        if (image != null && !image.isEmpty()) {
            product.setImage(image.getBytes());
            product.setImageType(image.getContentType());
        }

        UserProduct saved = userProductRepository.save(product);
        log.info("Custom product created with ID: {}", saved.getId());

        return saved;
    }

    @Override
    @Transactional
    public UserProduct adoptCatalogProduct(Long userId, Long catalogProductId, UserProductDTO dto, MultipartFile image) throws IOException {
        log.info("User {} adopting catalog product: {}", userId, catalogProductId);

        if (userProductRepository.existsByUserIdAndCatalogProductIdAndIsDeletedFalse(userId, catalogProductId)) {
            throw new IllegalArgumentException("Product already adopted");
        }

        CatalogProduct catalogProduct = catalogProductRepository.findById(catalogProductId)
                .orElseThrow(() -> new IllegalArgumentException("Catalog product not found"));

        if (!catalogProduct.getIsActive() || catalogProduct.getStatus() != CatalogProduct.CatalogStatus.VERIFIED) {
            throw new IllegalArgumentException("Catalog product is not available");
        }

        UserProduct userProduct = new UserProduct();
        userProduct.setUserId(userId);
        userProduct.setCatalogProduct(catalogProduct);
        // Denormalize the subcategory so reads resolve the category without touching the catalog
        if (catalogProduct.getSubcategory() != null) {
            userProduct.setSubcategoryId(catalogProduct.getSubcategory().getId());
        }
        // Prefer an explicitly provided barcode; fall back to the catalog product's
        userProduct.setBarcode(dto.getBarcode() != null ? dto.getBarcode() : catalogProduct.getBarcode());
        userProduct.setPrice(dto.getPrice());
        userProduct.setCostPrice(dto.getCostPrice());
        userProduct.setStock(dto.getStock() != null ? dto.getStock() : 0);
        userProduct.setMinStockLevel(dto.getMinStockLevel());
        userProduct.setMaxStockLevel(dto.getMaxStockLevel());
        userProduct.setUnit(dto.getUnit());
        userProduct.setSupplierId(dto.getSupplierId());
        userProduct.setIsDeleted(false);
        // Persisted server-side → confirmed write
        userProduct.setSynced(true);

        if (image != null && !image.isEmpty()) {
            userProduct.setImage(image.getBytes());
            userProduct.setImageType(image.getContentType());
        }

        UserProduct saved = userProductRepository.save(userProduct);

        catalogProduct.setAdoptionCount(catalogProduct.getAdoptionCount() + 1);
        catalogProductRepository.save(catalogProduct);

        log.info("Product adopted successfully with ID: {}", saved.getId());
        return saved;
    }

    @Override
    @Transactional
    public UserProduct updateUserProduct(Long userId, Long productId, UserProductDTO dto, MultipartFile image) throws IOException {
        log.info("Updating product {} for user {}", productId, userId);

        UserProduct existing = userProductRepository.findByIdAndUserId(productId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Product not found"));

        if (dto.getCustomName() != null) {
            existing.setCustomName(dto.getCustomName());
        }
        if (dto.getBarcode() != null) {
            existing.setBarcode(dto.getBarcode());
        }
        // NOT NULL columns — only overwrite when the client sent a value
        if (dto.getPrice() != null) {
            existing.setPrice(dto.getPrice());
        }
        if (dto.getCostPrice() != null) {
            existing.setCostPrice(dto.getCostPrice());
        }
        if (dto.getStock() != null) {
            existing.setStock(dto.getStock());
        }
        if (dto.getMinStockLevel() != null) {
            existing.setMinStockLevel(dto.getMinStockLevel());
        }
        if (dto.getMaxStockLevel() != null) {
            existing.setMaxStockLevel(dto.getMaxStockLevel());
        }
        if (dto.getUnit() != null) {
            existing.setUnit(dto.getUnit());
        }
        if (dto.getSupplierId() != null) {
            existing.setSupplierId(dto.getSupplierId());
        }
        if (dto.getIsActive() != null) {
            existing.setIsActive(dto.getIsActive());
        }

        if (dto.getDescription() != null) {
            existing.setDescription(dto.getDescription());
        }
        if (dto.getTags() != null) {
            existing.setTags(dto.getTags());
        }
        if (image != null && !image.isEmpty()) {
            existing.setImage(image.getBytes());
            existing.setImageType(image.getContentType());
        }
        // Persisted server-side → confirmed write
        existing.setSynced(true);

        UserProduct updated = userProductRepository.save(existing);
        log.info("Product updated successfully");

        return updated;
    }

    @Override
    @Transactional
    public void deleteUserProduct(Long userId, Long productId) {
        log.info("Deleting product {} for user {}", productId, userId);

        UserProduct product = userProductRepository.findByIdAndUserId(productId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Product not found"));

        product.setIsDeleted(true);
        userProductRepository.save(product);

        log.info("Product soft-deleted successfully");
    }

    @Override
    @Transactional(readOnly = true)
    public Page<UserProductResponseDTO> searchUserProducts(Long userId, String query, Pageable pageable) {
        Page<UserProduct> products = userProductRepository.searchUserProducts(userId, query, pageable);
        Map<Integer, Subcategory> subcategoriesById = findSubcategoriesWithCategory(products.getContent());
        return products.map(p -> toResponse(p, subcategoriesById));
    }

    @Override
    public UserProduct uploadProductImage(Long userId, Long productId, byte[] image, String imageType) {
        return null;
    }

    /**
     * Resolves the subcategories referenced by the products' denormalized
     * subcategoryId column in a single query (with categories fetched), so
     * mapping a page never triggers per-row lookups.
     */
    private Map<Integer, Subcategory> findSubcategoriesWithCategory(Collection<UserProduct> products) {
        Set<Integer> ids = products.stream()
                .map(UserProduct::getSubcategoryId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        if (ids.isEmpty()) {
            return Map.of();
        }

        return subcategoryRepository.findWithCategoryByIdIn(ids).stream()
                .collect(Collectors.toMap(Subcategory::getId, Function.identity()));
    }

    private UserProductResponseDTO toResponse(UserProduct product, Map<Integer, Subcategory> subcategoriesById) {
        return UserProductResponseDTO.from(product, resolveSubcategory(product, subcategoriesById));
    }

    /**
     * Custom products carry their subcategory in the subcategoryId column;
     * products adopted before that column was populated fall back to the
     * catalog product's subcategory (lazy load, safe inside the transaction).
     */
    private Subcategory resolveSubcategory(UserProduct product, Map<Integer, Subcategory> subcategoriesById) {
        if (product.getSubcategoryId() != null) {
            return subcategoriesById.get(product.getSubcategoryId());
        }
        if (product.getCatalogProduct() != null) {
            return product.getCatalogProduct().getSubcategory();
        }
        return null;
    }
}