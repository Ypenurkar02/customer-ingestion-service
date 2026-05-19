package com.cittaai.customer_ingestion_service;

import com.cittaai.customer_ingestion_service.dto.CustomerIngestionRequest;
import com.cittaai.customer_ingestion_service.dto.CustomerIngestionResponse;
import com.cittaai.customer_ingestion_service.service.CustomerIngestionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Testcontainers(disabledWithoutDocker = true)
class CustomerIngestionIntegrationTests {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void postgresProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.docker.compose.enabled", () -> "false");
    }

    @Autowired
    private CustomerIngestionService customerIngestionService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void cleanCustomers() {
        jdbcTemplate.execute("TRUNCATE TABLE customers RESTART IDENTITY");
    }

    @Test
    void ingestsOnlyNewCustomersAndIsIdempotent() {
        List<CustomerIngestionRequest> requests = List.of(
                customer("cust_001", "Alice", "alice@example.com", "US", "ACTIVE"),
                customer("cust_002", "Bob", "bob@example.com", "IN", "INACTIVE"));

        CustomerIngestionResponse firstResponse = customerIngestionService.ingest(requests, false);
        CustomerIngestionResponse secondResponse = customerIngestionService.ingest(requests, false);

        assertThat(firstResponse.received()).isEqualTo(2);
        assertThat(firstResponse.inserted()).isEqualTo(2);
        assertThat(firstResponse.failed()).isZero();
        assertThat(secondResponse.inserted()).isZero();
        assertThat(secondResponse.skippedExisting()).isEqualTo(2);
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM customers", Integer.class)).isEqualTo(2);
    }

    @Test
    void reportsValidationFailuresWithoutRejectingWholeBatch() {
        List<CustomerIngestionRequest> requests = List.of(
                customer("cust_101", "Valid", "valid@example.com", "UK", "ACTIVE"),
                customer("cust_102", "Bad Country", "bad-country@example.com", "ZZ", "ACTIVE"),
                customer(null, "Missing External", "missing@example.com", "US", "ACTIVE"));

        CustomerIngestionResponse response = customerIngestionService.ingest(requests, false);

        assertThat(response.inserted()).isEqualTo(1);
        assertThat(response.failed()).isEqualTo(2);
        assertThat(response.failedRecords())
                .extracting("reason")
                .contains("invalid country_code: ZZ", "external_id is required");
    }

    @Test
    void dryRunDoesNotPersistCustomers() {
        List<CustomerIngestionRequest> requests = List.of(
                customer("dry_001", "Dry Run", "dry@example.com", "US", "ACTIVE"));

        CustomerIngestionResponse response = customerIngestionService.ingest(requests, true);

        assertThat(response.dryRun()).isTrue();
        assertThat(response.inserted()).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM customers WHERE external_id = 'dry_001'",
                Integer.class)).isZero();
    }

    private CustomerIngestionRequest customer(
            String externalId,
            String name,
            String email,
            String countryCode,
            String statusCode) {
        return new CustomerIngestionRequest(externalId, name, email, countryCode, statusCode);
    }
}
