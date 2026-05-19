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
        @JsonProperty("duplicate_external_ids_truncated") boolean duplicateExternalIdsTruncated,
        @JsonProperty("failed_records") List<FailedRecord> failedRecords,
        @JsonProperty("failed_records_truncated") boolean failedRecordsTruncated,
        IngestionMetrics metrics) {
}
