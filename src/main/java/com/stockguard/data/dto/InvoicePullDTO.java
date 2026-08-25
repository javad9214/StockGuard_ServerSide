package com.stockguard.data.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Page;

import java.util.List;

/**
 * Pull-sync result. serverTime is captured on the server BEFORE the query
 * runs and becomes the client's next "since" cursor; combined with the
 * inclusive >= comparison on the sync query this guarantees a row written
 * during the pull is picked up by the next one (at worst re-sent once,
 * which the device tolerates by upserting on serverId).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class InvoicePullDTO {

    private long serverTime;        // epoch millis — next sync cursor
    private List<UserInvoiceResponseDTO> content;
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;
    private boolean last;

    public static InvoicePullDTO of(long serverTime, List<UserInvoiceResponseDTO> content, Page<?> page) {
        return new InvoicePullDTO(
                serverTime,
                content,
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isLast()
        );
    }
}
