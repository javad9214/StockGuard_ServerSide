package com.stockguard.domain;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "subcategories")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Subcategory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false)
    private String name;

    // Many subcategories belong to one category
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    private String icon; // optional
    private String description;

    @Column(nullable = false)
    private Long createdAt = System.currentTimeMillis();

    @Column(nullable = false)
    private Long updatedAt = System.currentTimeMillis();

    @Column(nullable = false)
    private Boolean isDeleted = false;
}
