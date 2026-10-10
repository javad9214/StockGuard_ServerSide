package com.stockguard.data.dto.sepehr;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class SepehrPagedDataDto {

    private List<SepehrProductDto> source;

    private Integer currentPage;

    private Integer totalPages;

    private Integer pageSize;

    private Long totalCount;
}
