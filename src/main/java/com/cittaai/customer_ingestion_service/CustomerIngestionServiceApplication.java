package com.cittaai.customer_ingestion_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class CustomerIngestionServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(CustomerIngestionServiceApplication.class, args);
	}

}
