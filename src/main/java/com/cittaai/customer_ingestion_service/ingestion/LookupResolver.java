package com.cittaai.customer_ingestion_service.ingestion;

import com.cittaai.customer_ingestion_service.config.IngestionProperties;
import com.cittaai.customer_ingestion_service.repository.CustomerJdbcRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;

@Component
public class LookupResolver {

    private final CustomerJdbcRepository repository;
    private final IngestionProperties ingestionProperties;
    private final Clock clock;
    private volatile CachedLookup cachedLookup;

    @Autowired
    public LookupResolver(CustomerJdbcRepository repository, IngestionProperties ingestionProperties) {
        this(repository, ingestionProperties, Clock.systemUTC());
    }

    LookupResolver(CustomerJdbcRepository repository, IngestionProperties ingestionProperties, Clock clock) {
        this.repository = repository;
        this.ingestionProperties = ingestionProperties;
        this.clock = clock;
    }

    public LookupCache loadLookupCache() {
        CachedLookup current = cachedLookup;
        Instant now = clock.instant();
        if (current != null && now.isBefore(current.expiresAt())) {
            return current.lookupCache();
        }

        synchronized (this) {
            current = cachedLookup;
            now = clock.instant();
            if (current != null && now.isBefore(current.expiresAt())) {
                return current.lookupCache();
            }

            LookupCache refreshed = new LookupCache(
                    repository.findCountryIdsByCode(),
                    repository.findStatusIdsByCode());
            cachedLookup = new CachedLookup(refreshed, now.plus(ingestionProperties.lookupCacheTtl()));
            return refreshed;
        }
    }

    private record CachedLookup(LookupCache lookupCache, Instant expiresAt) {
    }
}
