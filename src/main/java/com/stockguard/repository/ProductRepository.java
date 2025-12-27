package com.stockguard.repository;


import com.stockguard.data.entity.UserProduct;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<UserProduct, Long> {


    Optional<UserProduct> findByBarcode(String barcode);

    Page<UserProduct> findByNameContainingIgnoreCaseOrBarcodeContaining(String name, String barcode, Pageable pageable);
}
