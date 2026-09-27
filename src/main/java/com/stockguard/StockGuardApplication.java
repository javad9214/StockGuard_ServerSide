package com.stockguard;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@Slf4j
@SpringBootApplication
@EnableFeignClients(basePackages = "com.stockguard.client")
public class StockGuardApplication {

	public static void main(String[] args) {
		// journalctl shows no version banner for plain builds — log the
		// manifest version so a deployed instance is always identifiable
		log.info("Starting StockGuard server v{}",
                StockGuardApplication.class.getPackage().getImplementationVersion());
		SpringApplication.run(StockGuardApplication.class, args);
	}

}
