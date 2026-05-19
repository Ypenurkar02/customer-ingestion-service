package com.cittaai.customer_ingestion_service.ingestion;

public record ResolvedCustomer(
        String externalId,
        String name,
        String email,
        Long countryId,
        Long statusId) {
}
