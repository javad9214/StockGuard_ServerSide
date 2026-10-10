package com.stockguard.client;

import org.springframework.cloud.openfeign.FeignClient;

@FeignClient(name = "daryamart", url = "${daryamart.api.base-url}")
public interface DaryamartClient extends SepehrShopApi {
}
