package com.stockguard.repository;

import com.stockguard.data.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {


    Optional<Product> findByBarcode(String barcode);

    Page<Product> findByNameContainingIgnoreCaseOrBarcodeContaining(String name, String barcode, Pageable pageable);
}
