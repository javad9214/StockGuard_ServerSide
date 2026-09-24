package com.stockguard.data.dto.daryamart;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class DaryamartSearchResponseDto {

    private DaryamartPagedDataDto data;

    @JsonProperty("isSuccess")
    private Boolean success;

    private Integer statusCode;

    private String message;
}
