package com.cittaai.customer_ingestion_service.ingestion;

import com.cittaai.customer_ingestion_service.dto.FailedRecord;

import java.util.List;

public record ChunkResult(
        int inserted,
        int skippedExisting,
        int failed,
        int rowsScanned,
        int cacheHits,
        List<String> duplicateExternalIds,
        List<FailedRecord> failedRecords) {
}
