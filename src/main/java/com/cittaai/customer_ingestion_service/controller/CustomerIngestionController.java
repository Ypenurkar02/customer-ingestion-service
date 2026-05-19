package com.cittaai.customer_ingestion_service.controller;

import com.cittaai.customer_ingestion_service.dto.CustomerIngestionRequest;
import com.cittaai.customer_ingestion_service.dto.CustomerIngestionResponse;
import com.cittaai.customer_ingestion_service.service.CustomerIngestionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@Tag(name = "Customer Ingestion")
public class CustomerIngestionController {

    private final CustomerIngestionService customerIngestionService;

    public CustomerIngestionController(CustomerIngestionService customerIngestionService) {
        this.customerIngestionService = customerIngestionService;
    }

    @PostMapping(value = "/customers/ingest", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
            summary = "Ingest customers using delta detection and lookup resolution",
            description = """
                    Ingests a batch of customer records.

                    Supported lookup values:
                    - country_code: US, IN, UK
                    - status_code: ACTIVE, INACTIVE

                    Records with unsupported lookup values are reported in failed_records without rejecting the entire batch.
                    """)
    public CustomerIngestionResponse ingestCustomers(
            @RequestBody List<CustomerIngestionRequest> requests,
            @Parameter(description = "When true, validates and computes the delta without inserting records.")
            @RequestParam(defaultValue = "false") boolean dryRun) {
        return customerIngestionService.ingest(requests, dryRun);
    }
}
