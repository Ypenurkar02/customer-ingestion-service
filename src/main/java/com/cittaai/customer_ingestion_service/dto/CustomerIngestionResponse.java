package com.cittaai.customer_ingestion_service.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record CustomerIngestionResponse(
        int received,
        int inserted,
        @JsonProperty("skipped_existing") int skippedExisting,
        int failed,
        @JsonProperty("duration_ms") long durationMs,
        @JsonProperty("dry_run") boolean dryRun,
        @JsonProperty("duplicate_external_ids") List<String> duplicateExternalIds,
        @JsonProperty("failed_records") List<FailedRecord> failedRecords,
        IngestionMetrics metrics) {
}
