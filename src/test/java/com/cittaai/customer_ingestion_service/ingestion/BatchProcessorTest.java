package com.cittaai.customer_ingestion_service.ingestion;

import com.cittaai.customer_ingestion_service.dto.CustomerIngestionRequest;
import com.cittaai.customer_ingestion_service.repository.CustomerJdbcRepository;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class BatchProcessorTest {

    private final LookupCache lookupCache = new LookupCache(
            Map.of("US", 1L, "IN", 2L),
            Map.of("ACTIVE", 10L, "INACTIVE", 11L));

    @Test
    void rejectsDuplicateAndInvalidLookupRecordsBeforeInsert() {
        FakeCustomerJdbcRepository repository = new FakeCustomerJdbcRepository(Set.of());
        BatchProcessor batchProcessor = new BatchProcessor(new DeltaDetector(repository), repository);

        List<IndexedCustomerRecord> records = List.of(
                indexed(0, "cust_001", "US", "ACTIVE"),
                indexed(1, "cust_001", "US", "ACTIVE"),
                indexed(2, "cust_002", "XX", "ACTIVE"),
                indexed(3, "cust_003", "IN", "UNKNOWN"));

        ChunkResult result = batchProcessor.processChunk(records, lookupCache, new java.util.HashSet<>(), false);

        assertThat(result.inserted()).isEqualTo(1);
        assertThat(result.failed()).isEqualTo(3);
        assertThat(result.duplicateExternalIds()).containsExactly("cust_001");
        assertThat(result.failedRecords())
                .extracting("reason")
                .contains(
                        "duplicate external_id in payload",
                        "invalid country_code: XX",
                        "invalid status_code: UNKNOWN");
        assertThat(repository.bulkInsertCalled).isTrue();
    }

    @Test
    void skipsExistingExternalIdsAndDoesNotInsertDuringDryRun() {
        FakeCustomerJdbcRepository repository = new FakeCustomerJdbcRepository(Set.of("cust_001"));
        BatchProcessor batchProcessor = new BatchProcessor(new DeltaDetector(repository), repository);

        List<IndexedCustomerRecord> records = List.of(
                indexed(0, "cust_001", "US", "ACTIVE"),
                indexed(1, "cust_002", "IN", "INACTIVE"));

        ChunkResult result = batchProcessor.processChunk(records, lookupCache, new java.util.HashSet<>(), true);

        assertThat(result.inserted()).isEqualTo(1);
        assertThat(result.skippedExisting()).isEqualTo(1);
        assertThat(result.failed()).isZero();
        assertThat(repository.bulkInsertCalled).isFalse();
    }

    private IndexedCustomerRecord indexed(int index, String externalId, String countryCode, String statusCode) {
        return new IndexedCustomerRecord(index, new CustomerIngestionRequest(
                externalId,
                "Customer " + index,
                "customer" + index + "@example.com",
                countryCode,
                statusCode));
    }

    private static final class FakeCustomerJdbcRepository extends CustomerJdbcRepository {

        private final Set<String> existingExternalIds;
        private boolean bulkInsertCalled;

        private FakeCustomerJdbcRepository(Set<String> existingExternalIds) {
            super(null, null);
            this.existingExternalIds = existingExternalIds;
        }

        @Override
        public Set<String> findExistingExternalIds(Collection<String> externalIds) {
            return existingExternalIds;
        }

        @Override
        public int bulkInsertCustomers(List<ResolvedCustomer> customers) {
            bulkInsertCalled = true;
            return customers.size();
        }
    }
}
