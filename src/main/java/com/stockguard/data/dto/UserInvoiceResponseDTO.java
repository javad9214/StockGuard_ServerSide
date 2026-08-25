package com.stockguard.data.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.stockguard.data.entity.UserInvoice;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

/**
 * Items are null (and omitted from JSON) in list responses and only populated
 * for single-invoice reads and pull-sync, where they are actually needed.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserInvoiceResponseDTO {

    private Long id;                // server id — the device stores it as serverId
    private Long localId;

    private String prefix;
    private Long invoiceNumber;
    private Long invoiceDate;       // epoch millis
    private String invoiceType;
    private Long customerId;

    private Long totalAmount;
    private Long totalProfit;
    private Long totalDiscount;

    private String status;
    private String paymentMethod;
    private String notes;

    private Boolean isDeleted;
    private Long createdAt;         // epoch millis
    private Long updatedAt;         // epoch millis

    private List<UserInvoiceItemDTO> items;

    public static UserInvoiceResponseDTO summary(UserInvoice invoice) {
        return from(invoice, null);
    }

    public static UserInvoiceResponseDTO detail(UserInvoice invoice) {
        return from(invoice, invoice.getItems().stream()
                .map(UserInvoiceItemDTO::from)
                .toList());
    }

    public static UserInvoiceResponseDTO from(UserInvoice invoice, List<UserInvoiceItemDTO> items) {
        return new UserInvoiceResponseDTO(
                invoice.getId(),
                invoice.getLocalId(),
                invoice.getPrefix(),
                invoice.getInvoiceNumber(),
                invoice.getInvoiceDate(),
                invoice.getInvoiceType(),
                invoice.getCustomerId(),
                invoice.getTotalAmount(),
                invoice.getTotalProfit(),
                invoice.getTotalDiscount(),
                invoice.getStatus(),
                invoice.getPaymentMethod(),
                invoice.getNotes(),
                invoice.getIsDeleted(),
                toEpochMillis(invoice.getCreatedAt()),
                toEpochMillis(invoice.getUpdatedAt()),
                items
        );
    }

    // Columns are LocalDateTime written by the JVM clock, so millis must go
    // through the same zone or the sync cursor round-trip drifts.
    private static Long toEpochMillis(LocalDateTime dateTime) {
        return dateTime == null ? null : dateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
    }
}
