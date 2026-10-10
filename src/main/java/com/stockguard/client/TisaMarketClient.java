package com.stockguard.client;

import org.springframework.cloud.openfeign.FeignClient;

/**
 * فروشگاه مواد غذایی تیسا (tisamarket.co) — a grocery store on the
 * same Sepehr platform as Daryamart, with its own catalog; used as the
 * second fallback when neither Daryamart nor Seh Sooteh carries the
 * barcode.
 */
@FeignClient(name = "tisamarket", url = "${tisamarket.api.base-url}")
public interface TisaMarketClient extends SepehrShopApi {
}
