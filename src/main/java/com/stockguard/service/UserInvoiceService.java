package com.stockguard.service;

import com.stockguard.data.dto.InvoicePullDTO;
import com.stockguard.data.dto.SyncedInvoiceDTO;
import com.stockguard.data.dto.UserInvoiceDTO;
import com.stockguard.data.dto.UserInvoiceResponseDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface UserInvoiceService {

    Page<UserInvoiceResponseDTO> getUserInvoices(Long userId, Pageable pageable);

    Optional<UserInvoiceResponseDTO> getUserInvoiceById(Long userId, Long invoiceId);

    /**
     * Push sync: upserts a batch of device invoices by (userId, localId).
     * Returns the localId → serverId mapping so the device can mark them synced.
     */
    List<SyncedInvoiceDTO> pushInvoices(Long userId, List<UserInvoiceDTO> batch);

    /**
     * Pull sync: invoices (with items) whose server updatedAt >= since,
     * including soft-deleted ones so the device can delete locally.
     */
    InvoicePullDTO pullInvoices(Long userId, long sinceEpochMillis, Pageable pageable);

    void deleteUserInvoice(Long userId, Long invoiceId);
}
