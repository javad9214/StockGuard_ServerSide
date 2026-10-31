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

    Optional<CatalogProduct> findByExternalSourceAndExternalSourceId(
            String externalSource,
            Long externalSourceId
    );

    /**
     * SQL twin of CatalogCompleteness's missing-field predicates — active
     * rows still missing a real title, sell price or a stored (MinIO-key)
     * image. Shared by the admin backlog query and the batch-enrichment
     * ID fetch; keep in sync with the Java side.
     */
    String INCOMPLETE_PREDICATE = """
            cp.isActive = true
            AND (
                cp.suggestedSellPrice IS NULL
                OR cp.name IS NULL OR TRIM(cp.name) = '' OR cp.name = :unnamedPlaceholder
                OR cp.imageKey IS NULL OR TRIM(cp.imageKey) = ''
                   OR cp.imageKey LIKE 'http://%' OR cp.imageKey LIKE 'https://%'
            )""";

    /**
     * Admin incomplete-products backlog, most recently looked-up first;
     * rows never looked-up since the column was introduced fall back to
     * creation time.
     */
    @Query(
            value = """
        SELECT cp FROM CatalogProduct cp
        JOIN FETCH cp.subcategory s
        JOIN FETCH s.category
        WHERE """ + INCOMPLETE_PREDICATE + """
        ORDER BY COALESCE(cp.lastLookupAt, cp.createdAt) DESC
    """,
            countQuery = """
        SELECT COUNT(cp) FROM CatalogProduct cp
        WHERE """ + INCOMPLETE_PREDICATE
    )
    Page<CatalogProduct> findIncomplete(
            @Param("unnamedPlaceholder") String unnamedPlaceholder,
            Pageable pageable
    );

    /**
     * IDs of every incomplete row, ordered by primary key so the ordering
     * stays stable while the batch mutates lastLookupAt. Each row is
     * re-checked before enriching — some may be completed by user scans
     * while the batch works through the list.
     */
    @Query("SELECT cp.id FROM CatalogProduct cp WHERE " + INCOMPLETE_PREDICATE + " ORDER BY cp.id")
    List<Long> findIncompleteIds(@Param("unnamedPlaceholder") String unnamedPlaceholder);
}