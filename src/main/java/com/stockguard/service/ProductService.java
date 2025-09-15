package com.stockguard.service;

import com.stockguard.domain.Product;
import com.stockguard.repository.ProductRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ProductService {

    private final ProductRepository productRepository;

    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    public List<Product> getAllProducts() {
        return productRepository.findAll();
    }

    public Optional<Product> getProductById(Long id) {
        return productRepository.findById(id);
    }

    public Product createProduct(Product product) {
        return productRepository.save(product);
    }

    public Product updateProduct(Long id, Product updatedProduct) {
        return productRepository.findById(id)
                .map(existing -> {
                    existing.setName(updatedProduct.getName());
                    existing.setBarcode(updatedProduct.getBarcode());
                    existing.setPrice(updatedProduct.getPrice());
                    existing.setCostPrice(updatedProduct.getCostPrice());
                    existing.setDescription(updatedProduct.getDescription());
                    existing.setImage(updatedProduct.getImage());
                    existing.setSubcategoryId(updatedProduct.getSubcategoryId());
                    existing.setSupplierId(updatedProduct.getSupplierId());
                    existing.setUnit(updatedProduct.getUnit());
                    existing.setStock(updatedProduct.getStock());
                    existing.setMinStockLevel(updatedProduct.getMinStockLevel());
                    existing.setMaxStockLevel(updatedProduct.getMaxStockLevel());
                    existing.setIsActive(updatedProduct.getIsActive());
                    existing.setTags(updatedProduct.getTags());
                    existing.setLastSoldDate(updatedProduct.getLastSoldDate());
                    existing.setDate(updatedProduct.getDate());
                    existing.setSynced(updatedProduct.getSynced());
                    existing.setUpdatedAt(updatedProduct.getUpdatedAt());
                    existing.setIsDeleted(updatedProduct.getIsDeleted());
                    return productRepository.save(existing);
                })
                .orElseThrow(() -> new RuntimeException("Product not found with id " + id));
    }

    public void deleteProduct(Long id) {
        productRepository.deleteById(id);
    }
}
