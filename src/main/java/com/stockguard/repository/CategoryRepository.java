package com.stockguard.repository;


import com.stockguard.data.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CategoryRepository extends JpaRepository<Category, Integer> {

    // Find category by name (for avoiding duplicates during import)
    Optional<Category> findByName(String name);

    List<Category> findByIsDeletedFalseOrderByNameAsc();
}
