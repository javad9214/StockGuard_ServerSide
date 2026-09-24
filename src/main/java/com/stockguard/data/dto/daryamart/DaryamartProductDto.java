package com.stockguard.data.dto.daryamart;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class DaryamartProductDto {

    private Long id;

    private String name;

    private String imageAddress;

    private Long price;
}
