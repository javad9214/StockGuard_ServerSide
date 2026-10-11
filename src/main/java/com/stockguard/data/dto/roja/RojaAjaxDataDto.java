package com.stockguard.data.dto.roja;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;

/** Payload section of the rojashop ajax search answer. */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class RojaAjaxDataDto {

    private List<RojaProductDto> products;
}
