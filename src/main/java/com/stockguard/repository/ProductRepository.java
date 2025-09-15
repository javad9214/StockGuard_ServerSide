package com.stockguard.repository;

import com.stockguard.domain.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
    // You automatically get CRUD methods like:
    // findAll(), findById(), save(), deleteById(), etc.
}
