package com.stockguard.data.dto.roja;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

/**
 * Envelope of the rojashop.com ajax search (POST /api/ajax_search with
 * form body term=&lt;barcode&gt;). The endpoint only answers requests
 * carrying same-origin headers (Origin/Referer/X-Requested-With).
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class RojaSearchResponseDto {

    private Boolean status;

    private RojaAjaxDataDto data;
}
