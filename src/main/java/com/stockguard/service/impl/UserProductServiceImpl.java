package com.stockguard.service.impl;

import com.stockguard.data.entity.CatalogProduct;
import com.stockguard.data.entity.UserProduct;
import com.stockguard.repository.CatalogProductRepository;
import com.stockguard.repository.UserProductRepository;
import com.stockguard.service.UserProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserProductServiceImpl implements UserProductService {

    private final UserProductRepository userProductRepository;
    private final CatalogProductRepository catalogProductRepository;

    @Override
    @Transactional(readOnly = true)
    public Page<UserProduct> getUserProducts(Long userId, Pageable pageable) {
        return userProductRepository.findByUserIdAndIsDeletedFalse(userId, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<UserProduct> getUserProductById(Long userId, Long productId) {
        return userProductRepository.findByIdAndUserId(productId, userId);
    }

    @Override
    @Transactional
    public UserProduct createCustomProduct(Long userId, UserProduct product) {
        log.info("Creating custom product for user: {}", userId);

        product.setUserId(userId);
        product.setCatalogProduct(null); // Custom product
        product.setIsDeleted(false);

        UserProduct saved = userProductRepository.save(product);
        log.info("Custom product created with ID: {}", saved.getId());

        return saved;
    }

    @Override
    @Transactional
    public UserProduct adoptCatalogProduct(Long userId, Long catalogProductId, UserProduct productData) {
        log.info("User {} adopting catalog product: {}", userId, catalogProductId);

        // Check if already adopted
        if (userProductRepository.existsByUserIdAndCatalogProductIdAndIsDeletedFalse(userId, catalogProductId)) {
            throw new IllegalArgumentException("Product already adopted");
        }

        // Get catalog product
        CatalogProduct catalogProduct = catalogProductRepository.findById(catalogProductId)
                .orElseThrow(() -> new IllegalArgumentException("Catalog product not found"));

        if (!catalogProduct.getIsActive() || catalogProduct.getStatus() != CatalogProduct.CatalogStatus.VERIFIED) {
            throw new IllegalArgumentException("Catalog product is not available");
        }

        // Create user product
        UserProduct userProduct = new UserProduct();
        userProduct.setUserId(userId);
        userProduct.setCatalogProduct(catalogProduct);
        userProduct.setPrice(productData.getPrice());
        userProduct.setCostPrice(productData.getCostPrice());
        userProduct.setStock(productData.getStock());
        userProduct.setMinStockLevel(productData.getMinStockLevel());
        userProduct.setMaxStockLevel(productData.getMaxStockLevel());
        userProduct.setUnit(productData.getUnit());
        userProduct.setSupplierId(productData.getSupplierId());
        userProduct.setIsDeleted(false);

        UserProduct saved = userProductRepository.save(userProduct);

        // Increment adoption count
        catalogProduct.setAdoptionCount(catalogProduct.getAdoptionCount() + 1);
        catalogProductRepository.save(catalogProduct);

        log.info("Product adopted successfully with ID: {}", saved.getId());
        return saved;
    }

    @Override
    @Transactional
    public UserProduct updateUserProduct(Long userId, Long productId, UserProduct product) {
        log.info("Updating product {} for user {}", productId, userId);

        UserProduct existing = userProductRepository.findByIdAndUserId(productId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Product not found"));

        // Update fields
        if (product.getCustomName() != null) {
            existing.setCustomName(product.getCustomName());
        }
        existing.setPrice(product.getPrice());
        existing.setCostPrice(product.getCostPrice());
        existing.setStock(product.getStock());
        existing.setMinStockLevel(product.getMinStockLevel());
        existing.setMaxStockLevel(product.getMaxStockLevel());
        existing.setUnit(product.getUnit());
        existing.setSupplierId(product.getSupplierId());
        existing.setIsActive(product.getIsActive());

        if (product.getDescription() != null) {
            existing.setDescription(product.getDescription());
        }
        if (product.getImage() != null) {
            existing.setImage(product.getImage());
        }
        if (product.getTags() != null) {
            existing.setTags(product.getTags());
        }

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
    public Page<UserProduct> searchUserProducts(Long userId, String query, Pageable pageable) {
        return userProductRepository.searchUserProducts(userId, query, pageable);
    }
}