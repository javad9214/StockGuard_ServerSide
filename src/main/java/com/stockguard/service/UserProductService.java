package com.stockguard.service;

import com.stockguard.data.entity.UserProduct;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

public interface UserProductService {

    Page<UserProduct> getUserProducts(Long userId, Pageable pageable);

    Optional<UserProduct> getUserProductById(Long userId, Long productId);

    UserProduct createCustomProduct(Long userId, UserProduct product);

    UserProduct adoptCatalogProduct(Long userId, Long catalogProductId, UserProduct productData);

    UserProduct updateUserProduct(Long userId, Long productId, UserProduct product);

    void deleteUserProduct(Long userId, Long productId);

    Page<UserProduct> searchUserProducts(Long userId, String query, Pageable pageable);
}

