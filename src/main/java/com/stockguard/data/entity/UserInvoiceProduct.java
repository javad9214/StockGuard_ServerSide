package com.stockguard.data.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "user_invoice_products", indexes = {
        @Index(name = "idx_invoice_product_invoice", columnList = "invoiceId"),
        @Index(name = "idx_invoice_product_product", columnList = "productId")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserInvoiceProduct {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // invoiceId is written through UserInvoice's unidirectional @OneToMany;
    // kept as a plain column here so items never load their parent.

    // Server-side UserProduct id (the device resolves its local products'
    // serverId before pushing). Prices are denormalized snapshots so the
    // invoice stays intact even if the product is later changed or deleted.
    @Column(nullable = false)
    private Long productId;

    @Column(nullable = false)
    private Integer quantity;

    @Column(nullable = false)
    private Long priceAtSale;

    @Column(nullable = false)
    private Long costPriceAtTransaction;

    @Column(nullable = false)
    private Long discount = 0L;

    @Column(nullable = false)
    private Long total;
}
