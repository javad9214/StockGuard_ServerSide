package com.stockguard.repository;

import com.stockguard.domain.Subcategory;
import com.stockguard.domain.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface SubcategoryRepository extends JpaRepository<Subcategory, Integer> {

    // Find subcategory by name and its category (to avoid duplicates)
    Optional<Subcategory> findByNameAndCategory(String name, Category category);
}
