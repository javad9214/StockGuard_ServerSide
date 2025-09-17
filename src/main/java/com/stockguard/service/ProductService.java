package com.stockguard.service;

import com.stockguard.domain.Product;

import java.util.List;
import java.util.Optional;


public interface ProductService {


    List<Product> getAllProducts();
    Optional<Product> getProductById(Long id);
    Product createProduct(Product product);
    Product updateProduct(Long id, Product product);
    void deleteProduct(Long id);

    List<Product> searchProducts(String query);

}
