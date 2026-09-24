package com.stockguard;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients(basePackages = "com.stockguard.client")
public class StockGuardApplication {

	public static void main(String[] args) {
		SpringApplication.run(StockGuardApplication.class, args);
	}

}
