package com.stockguard.repository;

import com.stockguard.data.entity.UserProduct;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserProductRepository extends JpaRepository<UserProduct, Long> {

    // Find user's products
    Page<UserProduct> findByUserIdAndIsDeletedFalse(Long userId, Pageable pageable);

    Optional<UserProduct> findByIdAndUserId(Long id, Long userId);

    // Search user's products
    @Query("SELECT up FROM UserProduct up WHERE up.userId = :userId AND up.isDeleted = false " +
            "AND (LOWER(up.customName) LIKE LOWER(CONCAT('%', :query, '%')) " +
            "OR LOWER(up.catalogProduct.name) LIKE LOWER(CONCAT('%', :query, '%')) " +
            "OR LOWER(up.barcode) LIKE LOWER(CONCAT('%', :query, '%')))")
    Page<UserProduct> searchUserProducts(@Param("userId") Long userId,
                                         @Param("query") String query,
                                         Pageable pageable);

    // Check if user already adopted a catalog product
    boolean existsByUserIdAndCatalogProductIdAndIsDeletedFalse(Long userId, Long catalogProductId);

    // Get products by catalog product (for adoption count)
    List<UserProduct> findByCatalogProductIdAndIsDeletedFalse(Long catalogProductId);

    // Count user's products
    long countByUserIdAndIsDeletedFalse(Long userId);

    // Id-only projection: push-sync validation checks that invoice line items
    // reference products the user owns without loading the rows (and their images)
    @Query("select p.id from UserProduct p where p.userId = :userId and p.id in :ids")
    List<Long> findIdsByUserIdAndIdIn(@Param("userId") Long userId, @Param("ids") Collection<Long> ids);

    // Find low stock products
    @Query("SELECT up FROM UserProduct up WHERE up.userId = :userId " +
            "AND up.isDeleted = false AND up.stock <= up.minStockLevel")
    List<UserProduct> findLowStockProducts(@Param("userId") Long userId);
}