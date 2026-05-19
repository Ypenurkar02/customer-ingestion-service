package com.cittaai.customer_ingestion_service.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record CustomerIngestionRequest(
        @JsonProperty("external_id") String externalId,
        String name,
        String email,
        @JsonProperty("country_code") String countryCode,
        @JsonProperty("status_code") String statusCode) {
}
