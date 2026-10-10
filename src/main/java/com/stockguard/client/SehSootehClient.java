package com.stockguard.client;

import org.springframework.cloud.openfeign.FeignClient;

/**
 * سوپرمارکت سه سوته اصفهان (3sooteh.com) — a supermarket chain on the
 * same Sepehr platform as Daryamart, with its own catalog; used as the
 * first fallback when Daryamart does not carry the barcode.
 */
@FeignClient(name = "sehsooteh", url = "${sehsooteh.api.base-url}")
public interface SehSootehClient extends SepehrShopApi {
}
