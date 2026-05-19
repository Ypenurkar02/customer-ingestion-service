package com.cittaai.customer_ingestion_service.service;

import com.cittaai.customer_ingestion_service.config.IngestionProperties;
import com.cittaai.customer_ingestion_service.dto.CustomerIngestionRequest;
import com.cittaai.customer_ingestion_service.dto.CustomerIngestionResponse;
import com.cittaai.customer_ingestion_service.dto.FailedRecord;
import com.cittaai.customer_ingestion_service.dto.IngestionMetrics;
import com.cittaai.customer_ingestion_service.ingestion.BatchProcessor;
import com.cittaai.customer_ingestion_service.ingestion.ChunkResult;
import com.cittaai.customer_ingestion_service.ingestion.IndexedCustomerRecord;
import com.cittaai.customer_ingestion_service.ingestion.LookupCache;
import com.cittaai.customer_ingestion_service.ingestion.LookupResolver;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
public class CustomerIngestionService {

    private final IngestionProperties ingestionProperties;
    private final LookupResolver lookupResolver;
    private final BatchProcessor batchProcessor;
    private final TransactionTemplate transactionTemplate;

    public CustomerIngestionService(
            IngestionProperties ingestionProperties,
            LookupResolver lookupResolver,
            BatchProcessor batchProcessor,
            TransactionTemplate transactionTemplate) {
        this.ingestionProperties = ingestionProperties;
        this.lookupResolver = lookupResolver;
        this.batchProcessor = batchProcessor;
        this.transactionTemplate = transactionTemplate;
    }

    public CustomerIngestionResponse ingest(List<CustomerIngestionRequest> requests, boolean dryRun) {
        Instant startedAt = Instant.now();
        List<CustomerIngestionRequest> safeRequests = requests == null ? List.of() : requests;
        LookupCache lookupCache = lookupResolver.loadLookupCache();
        Set<String> seenExternalIds = new HashSet<>();

        int inserted = 0;
        int skippedExisting = 0;
        int failed = 0;
        int rowsScanned = 0;
        int cacheHits = 0;
        int chunksProcessed = 0;
        List<FailedRecord> failedRecords = new ArrayList<>();
        Set<String> duplicateExternalIds = new LinkedHashSet<>();

        for (int start = 0; start < safeRequests.size(); start += ingestionProperties.chunkSize()) {
            int end = Math.min(start + ingestionProperties.chunkSize(), safeRequests.size());
            List<IndexedCustomerRecord> chunk = indexedChunk(safeRequests, start, end);
            ChunkResult chunkResult = transactionTemplate.execute(status ->
                    batchProcessor.processChunk(chunk, lookupCache, seenExternalIds, dryRun));

            if (chunkResult == null) {
                continue;
            }

            chunksProcessed++;
            inserted += chunkResult.inserted();
            skippedExisting += chunkResult.skippedExisting();
            failed += chunkResult.failed();
            rowsScanned += chunkResult.rowsScanned();
            cacheHits += chunkResult.cacheHits();
            failedRecords.addAll(chunkResult.failedRecords());
            duplicateExternalIds.addAll(chunkResult.duplicateExternalIds());
        }

        long durationMs = Duration.between(startedAt, Instant.now()).toMillis();
        return new CustomerIngestionResponse(
                safeRequests.size(),
                inserted,
                skippedExisting,
                failed,
                durationMs,
                dryRun,
                List.copyOf(duplicateExternalIds),
                List.copyOf(failedRecords),
                new IngestionMetrics(chunksProcessed, rowsScanned, inserted, cacheHits));
    }

    private List<IndexedCustomerRecord> indexedChunk(List<CustomerIngestionRequest> requests, int start, int end) {
        List<IndexedCustomerRecord> chunk = new ArrayList<>(end - start);
        for (int i = start; i < end; i++) {
            chunk.add(new IndexedCustomerRecord(i, requests.get(i)));
        }
        return chunk;
    }
}
