package com.stockguard.data.dto;

import lombok.Data;

import java.util.List;

/**
 * Push-sync payload: an invoice exactly as it exists on the device.
 * localId identifies it for upsert; isDeleted pushes deletions too.
 */
@Data
public class UserInvoiceDTO {

    private Long localId;

    private String prefix;
    private Long invoiceNumber;
    private Long invoiceDate;        // epoch millis, user-chosen date
    private String invoiceType;      // "S" = sale, "B" = buy
    private Long customerId;

    private Long totalAmount;
    private Long totalProfit;
    private Long totalDiscount;

    private String status;
    private String paymentMethod;
    private String notes;

    private Boolean isDeleted;

    private List<UserInvoiceItemDTO> items;
}
