package com.stockguard.data.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "catalog_products", indexes = {
        @Index(name = "idx_barcode", columnList = "barcode"),
        @Index(name = "idx_status", columnList = "status"),
        @Index(name = "idx_normalized_name", columnList = "normalizedName")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CatalogProduct {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(unique = true)
    private String barcode;

    @Column(columnDefinition = "TEXT")
    private String description;

    private String brand;
    private String manufacturer;
    private String category;
    private String subcategory;
    private String imageUrl;

    private Long suggestedPrice; // Reference price only

    private String unit; // "piece", "kg", "liter"

    @Column(columnDefinition = "TEXT")
    private String tags;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CatalogStatus status = CatalogStatus.VERIFIED;

    private Long createdBy; // User ID who suggested (null if admin)
    private Long verifiedBy; // Admin ID who verified
    private LocalDateTime verifiedAt;

    @Column(nullable = false)
    private String normalizedName; // For duplicate detection

    @Column(nullable = false)
    private Integer qualityScore = 0;

    @Column(nullable = false)
    private Integer adoptionCount = 0;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @Column(nullable = false)
    private Boolean isActive = true;

    public enum CatalogStatus {
        VERIFIED,
        PENDING_REVIEW,
        REJECTED,
        DRAFT
    }

    @PrePersist
    public void prePersist() {
        if (normalizedName == null && name != null) {
            normalizedName = name.toLowerCase().replaceAll("[^a-z0-9]", "");
        }
    }
}
