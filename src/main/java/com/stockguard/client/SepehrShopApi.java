package com.stockguard.client;

import com.stockguard.data.dto.sepehr.SepehrSearchResponseDto;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Shared contract of the Sepehr shop platform (فروشگاه ساز سپهر).
 * Every tenant — Daryamart, Seh Sooteh, Tisa Market — exposes the same
 * {@code /newapi/v1} API on its own domain; searching by the product's
 * barcode as {@code Key} is an exact match when the shop carries it.
 * Feign clients inherit this interface, so the mapping lives once.
 */
public interface SepehrShopApi {

    @GetMapping("/newapi/v1/Products/GetProductsSearch")
    SepehrSearchResponseDto searchProducts(
            @RequestParam("Key") String key,
            @RequestParam("PageNumber") int pageNumber,
            @RequestParam("PageSize") int pageSize);
}
