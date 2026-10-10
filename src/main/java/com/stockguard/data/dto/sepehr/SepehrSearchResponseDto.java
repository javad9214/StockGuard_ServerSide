package com.stockguard.data.dto.sepehr;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * Envelope shared by every shop running on the Sepehr platform
 * (فروشگاه ساز سپهر): Daryamart, Seh Sooteh and Tisa Market all
 * answer with the same {@code /newapi/v1/Products/GetProductsSearch}
 * shape, so one DTO set covers all three clients.
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class SepehrSearchResponseDto {

    private SepehrPagedDataDto data;

    @JsonProperty("isSuccess")
    private Boolean success;

    private Integer statusCode;

    private String message;
}
