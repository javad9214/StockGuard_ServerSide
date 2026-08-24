package com.stockguard.repository;

import com.stockguard.data.entity.Subcategory;
import com.stockguard.data.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface SubcategoryRepository extends JpaRepository<Subcategory, Integer> {

    // Find subcategory by name and its category (to avoid duplicates)
    Optional<Subcategory> findByNameAndCategory(String name, Category category);

    // Bulk load subcategories with their category in one query (avoids N+1 when mapping lists)
    @Query("SELECT DISTINCT s FROM Subcategory s LEFT JOIN FETCH s.category WHERE s.id IN :ids")
    List<Subcategory> findWithCategoryByIdIn(@Param("ids") Collection<Integer> ids);

    // All live subcategories with their category fetched, for the category picker
    @Query("SELECT DISTINCT s FROM Subcategory s JOIN FETCH s.category WHERE s.isDeleted = false ORDER BY s.name")
    List<Subcategory> findByIsDeletedFalseWithCategory();
}
