package com.stockguard.data.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "user_invoices",
        uniqueConstraints = @UniqueConstraint(name = "uq_user_invoice_local", columnNames = {"userId", "localId"}),
        indexes = {
                // Pull sync walks this index in (userId, updatedAt) order
                @Index(name = "idx_user_invoice_updated", columnList = "userId,updatedAt"),
                @Index(name = "idx_user_invoice_type", columnList = "userId,invoiceType")
        })
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserInvoice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    // The invoice's id on the device that created it. Upsert key for push sync:
    // a retried batch lands on the same server row instead of duplicating it.
    @Column(nullable = false)
    private Long localId;

    private String prefix = "INV";

    @Column(nullable = false)
    private Long invoiceNumber;

    // Epoch millis of the user-chosen invoice date (may differ from createdAt)
    @Column(nullable = false)
    private Long invoiceDate;

    // "S" = sale, "B" = buy
    private String invoiceType;

    // Device-local customer id; customers are not synced yet, so no FK
    private Long customerId;

    private Long totalAmount;
    private Long totalProfit;

    @Column(nullable = false)
    private Long totalDiscount = 0L;

    private String status;
    private String paymentMethod;
    private String notes;

    @Column(nullable = false)
    private Boolean isDeleted = false;

    // Line items are owned by the invoice: they are replaced wholesale on
    // every push update and die with the invoice (orphanRemoval).
    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JoinColumn(name = "invoiceId")
    private List<UserInvoiceProduct> items = new ArrayList<>();

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;
}
