package com.stockguard.client;

import com.stockguard.data.dto.daryamart.DaryamartSearchResponseDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "daryamart", url = "${daryamart.api.base-url}")
public interface DaryamartClient {

    @GetMapping("/newapi/v1/Products/GetProductsSearch")
    DaryamartSearchResponseDto searchProducts(
            @RequestParam("Key") String key,
            @RequestParam("PageNumber") int pageNumber,
            @RequestParam("PageSize") int pageSize);
}
