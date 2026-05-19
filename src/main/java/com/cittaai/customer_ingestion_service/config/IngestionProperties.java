package com.cittaai.customer_ingestion_service.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "ingestion")
public record IngestionProperties(
        int chunkSize,
        int maxReportedFailures,
        int maxReportedDuplicateExternalIds,
        Duration lookupCacheTtl) {

    public IngestionProperties {
        if (chunkSize <= 0) {
            chunkSize = 1000;
        }
        if (maxReportedFailures <= 0) {
            maxReportedFailures = 1000;
        }
        if (maxReportedDuplicateExternalIds <= 0) {
            maxReportedDuplicateExternalIds = 1000;
        }
        if (lookupCacheTtl == null || lookupCacheTtl.isNegative() || lookupCacheTtl.isZero()) {
            lookupCacheTtl = Duration.ofMinutes(5);
        }
    }
}
