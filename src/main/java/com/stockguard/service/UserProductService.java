package com.stockguard.service;

import com.stockguard.data.dto.UserProductDTO;
import com.stockguard.data.entity.UserProduct;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Optional;

public interface UserProductService {

    Page<UserProduct> getUserProducts(Long userId, Pageable pageable);

    Optional<UserProduct> getUserProductById(Long userId, Long productId);

    UserProduct createCustomProduct(Long userId, UserProductDTO productDTO, MultipartFile image) throws IOException;

    UserProduct adoptCatalogProduct(Long userId, Long catalogProductId, UserProductDTO productData, MultipartFile image) throws IOException;

    UserProduct updateUserProduct(Long userId, Long productId, UserProductDTO product, MultipartFile image) throws IOException;

    void deleteUserProduct(Long userId, Long productId);

    Page<UserProduct> searchUserProducts(Long userId, String query, Pageable pageable);

    UserProduct uploadProductImage(Long userId, Long productId, byte[] image, String imageType);
}

