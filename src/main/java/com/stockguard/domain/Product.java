package com.stockguard.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "products")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    private String barcode;

    private Long price;       // Selling price
    private Long costPrice;   // Cost price for buying

    private String description;

    private String image;

    private Integer subcategoryId;
    private Integer supplierId;

    private String unit;
    private Integer stock;
    private Integer minStockLevel;
    private Integer maxStockLevel;

    private Boolean isActive = true;
    private String tags;

    private LocalDateTime lastSoldDate;
    private LocalDateTime date;

    private Boolean synced = false;
    private LocalDateTime createdAt = LocalDateTime.now();
    private LocalDateTime updatedAt = LocalDateTime.now();
    private Boolean isDeleted = false;
}

