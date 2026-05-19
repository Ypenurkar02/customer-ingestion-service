package com.cittaai.customer_ingestion_service.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "ingestion")
public record IngestionProperties(int chunkSize) {

    public IngestionProperties {
        if (chunkSize <= 0) {
            chunkSize = 1000;
        }
    }
}
