package com.stockguard.repository;


import com.stockguard.data.entity.UserProduct;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<UserProduct, Long> {

    Optional<UserProduct> findByCatalogProduct_Barcode(String barcode);

    Page<UserProduct> findByCustomNameContainingIgnoreCaseOrCatalogProduct_BarcodeContaining(
            String name,
            String barcode,
            Pageable pageable
    );
}
