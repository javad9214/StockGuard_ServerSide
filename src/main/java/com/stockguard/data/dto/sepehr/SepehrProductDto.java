package com.stockguard.data.dto.sepehr;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

/**
 * Product row of the Sepehr platform search response. Prices are in
 * Toman (the platform UI renders the same value with a تومان label);
 * imageAddress is relative to the shop's own base URL.
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class SepehrProductDto {

    private Long id;

    private String name;

    private String imageAddress;

    private Long price;
}
