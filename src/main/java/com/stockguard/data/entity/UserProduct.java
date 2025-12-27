package com.stockguard.data.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_products", indexes = {
        @Index(name = "idx_user_id", columnList = "userId"),
        @Index(name = "idx_catalog_product_id", columnList = "catalogProductId"),
        @Index(name = "idx_user_catalog", columnList = "userId,catalogProductId")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserProduct {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    // If null → custom product, if set → adopted from catalog
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "catalogProductId")
    private CatalogProduct catalogProduct;

    private String customName; // Overrides catalog name

    @Column(nullable = false)
    private Long price; // User's selling price

    @Column(nullable = false)
    private Long costPrice; // User's cost price

    private String description;
    private String image;

    private Integer subcategoryId;
    private Integer supplierId;

    private String unit;

    @Column(nullable = false)
    private Integer stock = 0;

    private Integer minStockLevel;
    private Integer maxStockLevel;

    @Column(nullable = false)
    private Boolean isActive = true;

    private String tags;
    private LocalDateTime lastSoldDate;

    @Column(nullable = false)
    private Boolean synced = false;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @Column(nullable = false)
    private Boolean isDeleted = false;
}