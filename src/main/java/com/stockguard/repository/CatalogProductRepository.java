package com.stockguard.repository;

import com.stockguard.data.entity.CatalogProduct;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CatalogProductRepository extends JpaRepository<CatalogProduct, Long> {

    // Find by barcode
    Optional<CatalogProduct> findByBarcodeAndIsActiveTrue(String barcode);

    // Check barcode exists
    boolean existsByBarcode(String barcode);

    @Query(
            value = """
        SELECT p FROM CatalogProduct p
        JOIN FETCH p.subcategory s
        JOIN FETCH s.category
        WHERE p.status = :status AND p.isActive = true
    """,
            countQuery = "SELECT COUNT(p) FROM CatalogProduct p WHERE p.status = :status AND p.isActive = true"
    )
    Page<CatalogProduct> findByStatusAndIsActiveTrueWithJoins(
            @Param("status") CatalogProduct.CatalogStatus status,
            Pageable pageable
    );

    @Query(
            value = """
        SELECT cp FROM CatalogProduct cp
        JOIN FETCH cp.subcategory s
        JOIN FETCH s.category
        WHERE cp.isActive = true
          AND cp.status = com.stockguard.data.entity.CatalogProduct.CatalogStatus.VERIFIED
          AND (
              LOWER(cp.name)           LIKE LOWER(CONCAT('%', :query, '%'))
              OR LOWER(cp.brand)       LIKE LOWER(CONCAT('%', :query, '%'))
              OR LOWER(cp.normalizedName) LIKE LOWER(CONCAT('%', :query, '%'))
          )
    """,
            countQuery = """
        SELECT COUNT(cp) FROM CatalogProduct cp
        WHERE cp.isActive = true
          AND cp.status = com.stockguard.data.entity.CatalogProduct.CatalogStatus.VERIFIED
          AND (
              LOWER(cp.name)              LIKE LOWER(CONCAT('%', :query, '%'))
              OR LOWER(cp.brand)          LIKE LOWER(CONCAT('%', :query, '%'))
              OR LOWER(cp.normalizedName) LIKE LOWER(CONCAT('%', :query, '%'))
          )
    """
    )
    Page<CatalogProduct> searchCatalog(@Param("query") String query, Pageable pageable);


    // Find similar names (for duplicate detection)
    @Query("SELECT cp FROM CatalogProduct cp WHERE cp.isActive = true " +
            "AND cp.normalizedName = :normalizedName")
    List<CatalogProduct> findByNormalizedName(@Param("normalizedName") String normalizedName);

    Page<CatalogProduct> findBySubcategory_Category_NameAndStatusAndIsActiveTrue(
            String categoryName,
            CatalogProduct.CatalogStatus status,
            Pageable pageable
    );


    // Count by status
    long countByStatus(CatalogProduct.CatalogStatus status);

    // Top adopted products
    @Query("SELECT cp FROM CatalogProduct cp WHERE cp.status = 'VERIFIED' " +
            "AND cp.isActive = true ORDER BY cp.adoptionCount DESC, cp.qualityScore DESC")
    Page<CatalogProduct> findTopAdopted(Pageable pageable);

    // Pending review (for admin)
    @Query("SELECT cp FROM CatalogProduct cp WHERE cp.status = 'PENDING_REVIEW' " +
            "ORDER BY cp.createdAt ASC")
    Page<CatalogProduct> findPendingReview(Pageable pageable);


    boolean existsByExternalSourceAndExternalSourceId(
            String externalSource,
            Long externalSourceId
    );
}