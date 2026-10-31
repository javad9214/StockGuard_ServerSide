package com.stockguard.data.dto.barcode.response;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * Progress of the admin batch that re-runs shop enrichment over every
 * incomplete catalog product. Snapshotted from the single batch thread —
 * counters may be a hair stale, which never matters for a progress readout.
 */
@Getter
@Builder
public class EnrichmentBatchStatusDTO {

    private boolean running;

    /** Barcode being looked up right now (last one after the batch ends). */
    private String currentBarcode;

    private int total;
    private int processed;
    private int remaining;

    /** Products where at least one missing field was filled. */
    private int enriched;

    /** Products the shops still could not complete (the backlog remains). */
    private int stillIncomplete;

    /** Products skipped by an unexpected error (shop failures don't count). */
    private int failed;

    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;
}
