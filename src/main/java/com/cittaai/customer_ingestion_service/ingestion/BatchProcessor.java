package com.cittaai.customer_ingestion_service.ingestion;

import com.cittaai.customer_ingestion_service.dto.CustomerIngestionRequest;
import com.cittaai.customer_ingestion_service.dto.FailedRecord;
import com.cittaai.customer_ingestion_service.repository.CustomerJdbcRepository;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Component
public class BatchProcessor {

    private final DeltaDetector deltaDetector;
    private final CustomerJdbcRepository repository;

    public BatchProcessor(DeltaDetector deltaDetector, CustomerJdbcRepository repository) {
        this.deltaDetector = deltaDetector;
        this.repository = repository;
    }

    public ChunkResult processChunk(
            List<IndexedCustomerRecord> records,
            LookupCache lookupCache,
            Set<String> seenExternalIds,
            boolean dryRun) {

        List<FailedRecord> failedRecords = new ArrayList<>();
        List<String> duplicateExternalIds = new ArrayList<>();
        List<IndexedCustomerRecord> validRecords = new ArrayList<>();
        Set<String> validExternalIds = new LinkedHashSet<>();
        int cacheHits = 0;

        for (IndexedCustomerRecord indexedRecord : records) {
            CustomerIngestionRequest request = indexedRecord.request();
            String externalId = trimToNull(request.externalId());

            if (externalId == null) {
                failedRecords.add(new FailedRecord(indexedRecord.index(), null, "external_id is required"));
                continue;
            }

            if (!seenExternalIds.add(externalId)) {
                duplicateExternalIds.add(externalId);
                failedRecords.add(new FailedRecord(indexedRecord.index(), externalId, "duplicate external_id in payload"));
                continue;
            }

            String countryCode = trimToNull(request.countryCode());
            String statusCode = trimToNull(request.statusCode());

            Long countryId = lookupCache.countriesByCode().get(countryCode);
            Long statusId = lookupCache.statusesByCode().get(statusCode);
            cacheHits += (countryId == null ? 0 : 1) + (statusId == null ? 0 : 1);

            if (countryId == null) {
                failedRecords.add(new FailedRecord(indexedRecord.index(), externalId, "invalid country_code: " + countryCode));
                continue;
            }

            if (statusId == null) {
                failedRecords.add(new FailedRecord(indexedRecord.index(), externalId, "invalid status_code: " + statusCode));
                continue;
            }

            validRecords.add(indexedRecord);
            validExternalIds.add(externalId);
        }

        Set<String> existingExternalIds = deltaDetector.findExistingExternalIds(validExternalIds);
        List<ResolvedCustomer> customersToInsert = new ArrayList<>();

        for (IndexedCustomerRecord indexedRecord : validRecords) {
            CustomerIngestionRequest request = indexedRecord.request();
            String externalId = trimToNull(request.externalId());

            if (existingExternalIds.contains(externalId)) {
                continue;
            }

            customersToInsert.add(new ResolvedCustomer(
                    externalId,
                    trimToNull(request.name()),
                    trimToNull(request.email()),
                    lookupCache.countriesByCode().get(trimToNull(request.countryCode())),
                    lookupCache.statusesByCode().get(trimToNull(request.statusCode()))));
        }

        int inserted = dryRun ? customersToInsert.size() : repository.bulkInsertCustomers(customersToInsert);
        int skippedExisting = existingExternalIds.size() + (dryRun ? 0 : customersToInsert.size() - inserted);

        return new ChunkResult(
                inserted,
                skippedExisting,
                failedRecords.size(),
                validExternalIds.size(),
                cacheHits,
                List.copyOf(new HashSet<>(duplicateExternalIds)),
                List.copyOf(failedRecords));
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
