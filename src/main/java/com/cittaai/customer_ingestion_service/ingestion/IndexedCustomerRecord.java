package com.cittaai.customer_ingestion_service.ingestion;

import com.cittaai.customer_ingestion_service.dto.CustomerIngestionRequest;

public record IndexedCustomerRecord(int index, CustomerIngestionRequest request) {
}
