package com.cittaai.customer_ingestion_service.ingestion;

import java.util.Map;

public record LookupCache(Map<String, Long> countriesByCode, Map<String, Long> statusesByCode) {
}
