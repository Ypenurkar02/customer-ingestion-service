package com.cittaai.customer_ingestion_service.ingestion;

import com.cittaai.customer_ingestion_service.repository.CustomerJdbcRepository;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

@Component
public class DeltaDetector {

    private final CustomerJdbcRepository repository;

    public DeltaDetector(CustomerJdbcRepository repository) {
        this.repository = repository;
    }

    public Set<String> findExistingExternalIds(Collection<String> externalIds) {
        return new HashSet<>(repository.findExistingExternalIds(externalIds));
    }
}
