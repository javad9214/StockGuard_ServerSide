package com.stockguard.repository;

import com.stockguard.data.entity.UserInvoice;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserInvoiceRepository extends JpaRepository<UserInvoice, Long> {

    Page<UserInvoice> findByUserIdAndIsDeletedFalse(Long userId, Pageable pageable);

    // Upsert key for push sync
    Optional<UserInvoice> findByUserIdAndLocalId(Long userId, Long localId);

    @EntityGraph(attributePaths = "items")
    @Query("select distinct i from UserInvoice i where i.id = :id and i.userId = :userId")
    Optional<UserInvoice> findWithItemsByIdAndUserId(@Param("id") Long id, @Param("userId") Long userId);

    // Pull sync: page over ids first, then fetch that page's invoices with
    // their items — avoids both N+1 queries and in-memory pagination of a
    // join-fetched collection.
    @Query("select i.id from UserInvoice i where i.userId = :userId and i.updatedAt >= :since")
    Page<Long> findIdsByUserIdAndUpdatedAtGreaterThanEqual(@Param("userId") Long userId,
                                                           @Param("since") LocalDateTime since,
                                                           Pageable pageable);

    @EntityGraph(attributePaths = "items")
    @Query("select distinct i from UserInvoice i where i.id in :ids")
    List<UserInvoice> findWithItemsByIdIn(@Param("ids") Collection<Long> ids);

    long countByUserIdAndIsDeletedFalse(Long userId);
}
