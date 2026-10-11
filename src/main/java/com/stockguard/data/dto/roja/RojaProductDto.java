package com.stockguard.data.dto.roja;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

/**
 * Product row of the rojashop ajax search response. Carries no price and
 * no barcode field — the shop's image-naming convention (…/&lt;internal
 * id&gt;/&lt;BARCODE&gt;.jpg) is what ties the row to a barcode.
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class RojaProductDto {

    private Long id;

    private String title;

    private String image;

    private String url;
}
