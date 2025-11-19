package com.stockguard.service.impl;

import com.stockguard.data.entity.Product;
import com.stockguard.exception.ProductNotFoundException;
import com.stockguard.exception.ProductOperationException;
import com.stockguard.repository.ProductRepository;
import com.stockguard.service.ProductService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@Transactional
@Slf4j
public class ProductServiceImpl implements ProductService {

    @Autowired
    private final ProductRepository productRepository;


    public ProductServiceImpl(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @Override
    public Page<Product> getAllProducts(Pageable pageable) {
        return productRepository.findAll(pageable);
    }

    @Override
    public Optional<Product> getProductById(Long id) {
        return productRepository.findById(id);
    }

    @Override
    public Product createProduct(Product product) {
        try {
            log.info("Creating product: {}", product.getName());
            Product savedProduct = productRepository.save(product);
            log.info("Product created successfully with ID: {}", savedProduct.getId());
            return savedProduct;
        } catch (DataIntegrityViolationException e) {
            log.error("Data integrity violation while creating product: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error while creating product: {}", e.getMessage());
            throw new ProductOperationException("Failed to create product", e);
        }
    }

    @Override
    public Product updateProduct(Long id, Product product) {
        try {
            log.info("Updating product with ID: {}", id);

            // Check if product exists
            if (!productRepository.existsById(id)) {
                log.warn("Product with ID {} not found", id);
                throw new ProductNotFoundException("Product with ID " + id + " not found");
            }

            product.setId(id);
            Product updatedProduct = productRepository.save(product);
            log.info("Product with ID {} updated successfully", id);
            return updatedProduct;
        } catch (ProductNotFoundException e) {
            throw e;
        } catch (DataIntegrityViolationException e) {
            log.error("Data integrity violation while updating product {}: {}", id, e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error while updating product {}: {}", id, e.getMessage());
            throw new ProductOperationException("Failed to update product", e);
        }
    }

    @Override
    public void deleteProduct(Long id) {
        try {
            log.info("Deleting product with ID: {}", id);

            // Check if product exists
            if (!productRepository.existsById(id)) {
                log.warn("Product with ID {} not found", id);
                throw new ProductNotFoundException("Product with ID " + id + " not found");
            }

            productRepository.deleteById(id);
            log.info("Product with ID {} deleted successfully", id);
        } catch (ProductNotFoundException e) {
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error while deleting product {}: {}", id, e.getMessage());
            throw new ProductOperationException("Failed to delete product", e);
        }
    }

    @Override
    public Page<Product> searchProducts(String query, Pageable pageable) {
        return productRepository.findByNameContainingIgnoreCaseOrBarcodeContaining(query, query,pageable);
    }
}
