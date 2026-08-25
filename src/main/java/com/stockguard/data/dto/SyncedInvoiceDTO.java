package com.stockguard.data.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Push-sync result: lets the device map each pushed local invoice to its
 * server id and mark it synced.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SyncedInvoiceDTO {

    private Long localId;
    private Long serverId;
}
