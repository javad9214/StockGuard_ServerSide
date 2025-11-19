package com.stockguard.service;

import com.stockguard.data.entity.Product;

import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ProductService {


    Page<Product> getAllProducts(Pageable pageable);
    Optional<Product> getProductById(Long id);
    Product createProduct(Product product);
    Product updateProduct(Long id, Product product);
    void deleteProduct(Long id);

    Page<Product> searchProducts(String query, Pageable pageable);

}
