package com.stockguard.data.dto.daryamart;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class DaryamartPagedDataDto {

    private List<DaryamartProductDto> source;

    private Integer currentPage;

    private Integer totalPages;

    private Integer pageSize;

    private Long totalCount;
}
