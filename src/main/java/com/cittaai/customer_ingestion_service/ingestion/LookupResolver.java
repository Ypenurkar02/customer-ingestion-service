package com.cittaai.customer_ingestion_service.ingestion;

import com.cittaai.customer_ingestion_service.repository.CustomerJdbcRepository;
import org.springframework.stereotype.Component;

@Component
public class LookupResolver {

    private final CustomerJdbcRepository repository;

    public LookupResolver(CustomerJdbcRepository repository) {
        this.repository = repository;
    }

    public LookupCache loadLookupCache() {
        return new LookupCache(
                repository.findCountryIdsByCode(),
                repository.findStatusIdsByCode());
    }
}
