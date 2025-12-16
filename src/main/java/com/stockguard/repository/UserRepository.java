package com.stockguard.repository;

import com.stockguard.data.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByPhoneNumber(String phoneNumber);

    boolean existsByPhoneNumber(String phoneNumber);

    // Admin queries
    @Query("""
    SELECT u FROM User u
    WHERE
      (:search IS NULL OR
      u.fullName LIKE %:search% OR
      u.phoneNumber LIKE %:search%)
    AND (:enabled IS NULL OR u.enabled = :enabled)
    AND (:locked IS NULL OR u.accountLocked = :locked)
    """)
    Page<User> findAllWithFilters(
            @Param("search") String search,
            @Param("enabled") Boolean enabled,
            @Param("locked") Boolean locked,
            Pageable pageable
    );


    // Statistics queries
    long countByEnabled(Boolean enabled);

    long countByAccountLocked(Boolean locked);

    long countByCreatedAtAfter(LocalDateTime dateTime);

    @Query("SELECT COUNT(u) FROM User u WHERE u.enabled = true AND u.accountLocked = false")
    long countActiveUsers();
}