package com.cittaai.customer_ingestion_service.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record FailedRecord(
        int index,
        @JsonProperty("external_id") String externalId,
        String reason) {
}
