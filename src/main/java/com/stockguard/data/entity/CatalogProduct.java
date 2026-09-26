package com.stockguard.data.entity;

import com.stockguard.data.enums.Unit;
import com.stockguard.util.ImageUrlResolver;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "catalog_products",
        indexes = {
                @Index(name = "idx_barcode", columnList = "barcode"),
                @Index(name = "idx_status", columnList = "status"),
                @Index(name = "idx_normalized_name", columnList = "normalizedName"),
                @Index(name = "idx_external_source", columnList = "externalSource,externalSourceId"),
                @Index(name = "idx_subcategory", columnList = "subcategory_id")
        },
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_external_product",
                        columnNames = {"externalSource", "externalSourceId"}
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CatalogProduct {

    // ================= ID =================
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ================= CORE =================
    @Column(nullable = false)
    private String name;

    @Column(unique = true)
    private String barcode;

    @Column(columnDefinition = "TEXT")
    private String description;

    private String brand;
    private String manufacturer;

    // ================= CATEGORY RELATION =================
    /**
     * Product belongs to ONE subcategory
     * Category is reachable via subcategory.getCategory()
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subcategory_id", nullable = false)
    private Subcategory subcategory;

    // ================= IMAGE =================
    /**
     * MinIO object key for images uploaded through this service. Never a
     * full URL for our own uploads. Legacy rows (SNAPP/Daryamart imports)
     * still hold the external absolute URL — resolved at read time by
     * {@link #getImageUrl()}.
     */
    @Column(name = "image_key", columnDefinition = "TEXT")
    private String imageKey;

    @Column(nullable = false)
    private String imageSource; // SNAPP_MARKET, MANUAL, OTHER

    // ================= PRICE =================
    private Long suggestedSellPrice;
    private Long suggestedCostPrice;

    // ================= UNIT =================
    @Enumerated(EnumType.STRING)
    private Unit unit;

    // ================= META =================
    @Column(columnDefinition = "TEXT")
    private String tags;

    // ================= STATUS =================
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CatalogStatus status = CatalogStatus.VERIFIED;

    private Long createdBy;
    private Long verifiedBy;
    private LocalDateTime verifiedAt;

    // ================= DEDUP =================
    @Column(nullable = false)
    private String normalizedName;

    @Column(nullable = false)
    private Integer qualityScore = 0;

    @Column(nullable = false)
    private Integer adoptionCount = 0;

    // ================= EXTERNAL SOURCE =================
    @Column(nullable = false)
    private String externalSource; // SNAPP_MARKET

    @Column(nullable = false)
    private Long externalSourceId; // snapp product id

    // ================= LIFECYCLE =================
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @Column(nullable = false)
    private Boolean isActive = true;

    // ================= ENUM =================
    public enum CatalogStatus {
        VERIFIED,
        PENDING_REVIEW,
        REJECTED,
        DRAFT
    }

    // ================= HOOK =================
    @PrePersist
    public void prePersist() {
        if (normalizedName == null && name != null) {
            normalizedName = normalize(name, brand);
        }
    }

    /**
     * Computed, never persisted: the absolute URL clients load the image
     * from, built at serialization time from {@code app.base-url} + imageKey
     * (legacy external URLs pass through unchanged). Keeps the API contract
     * ({@code imageUrl}) stable while the DB stores only the key.
     */
    @Transient
    public String getImageUrl() {
        return ImageUrlResolver.resolve(imageKey);
    }

    private String normalize(String name, String brand) {
        String base = (name + " " + (brand != null ? brand : "")).toLowerCase();

        base = base
                .replace("‌", "") // half-space
                .replace("ي", "ی")
                .replace("ك", "ک");

        return base.replaceAll("[^a-z0-9آ-ی]", "");
    }
}
