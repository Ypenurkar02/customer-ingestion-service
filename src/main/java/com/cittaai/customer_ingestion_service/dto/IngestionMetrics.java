package com.cittaai.customer_ingestion_service.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record IngestionMetrics(
        @JsonProperty("chunks_processed") int chunksProcessed,
        @JsonProperty("rows_scanned") int rowsScanned,
        @JsonProperty("rows_inserted") int rowsInserted,
        @JsonProperty("cache_hits") int cacheHits) {
}
