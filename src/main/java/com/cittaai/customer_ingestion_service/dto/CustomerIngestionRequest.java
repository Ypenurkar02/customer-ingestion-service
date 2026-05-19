package com.cittaai.customer_ingestion_service.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Customer record to ingest. Valid lookup values are country_code: US, IN, UK and status_code: ACTIVE, INACTIVE.")
public record CustomerIngestionRequest(
        @JsonProperty("external_id")
        @Schema(description = "Unique customer identifier from the source system.", example = "cust_001", requiredMode = Schema.RequiredMode.REQUIRED)
        String externalId,

        @Schema(description = "Customer full name.", example = "Alice Johnson")
        String name,

        @Schema(description = "Customer email address.", example = "alice@example.com")
        String email,

        @JsonProperty("country_code")
        @Schema(
                description = "Country lookup code. Supported values are US, IN, and UK.",
                example = "US",
                allowableValues = {"US", "IN", "UK"},
                requiredMode = Schema.RequiredMode.REQUIRED)
        String countryCode,

        @JsonProperty("status_code")
        @Schema(
                description = "Customer status lookup code. Supported values are ACTIVE and INACTIVE.",
                example = "ACTIVE",
                allowableValues = {"ACTIVE", "INACTIVE"},
                requiredMode = Schema.RequiredMode.REQUIRED)
        String statusCode) {
}
