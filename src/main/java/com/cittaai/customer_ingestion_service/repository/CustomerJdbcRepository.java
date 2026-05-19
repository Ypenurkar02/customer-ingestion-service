package com.cittaai.customer_ingestion_service.repository;

import com.cittaai.customer_ingestion_service.ingestion.ResolvedCustomer;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Statement;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Repository
public class CustomerJdbcRepository {

    private final JdbcTemplate jdbcTemplate;
    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    public CustomerJdbcRepository(JdbcTemplate jdbcTemplate, NamedParameterJdbcTemplate namedParameterJdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        this.namedParameterJdbcTemplate = namedParameterJdbcTemplate;
    }

    public Map<String, Long> findCountryIdsByCode() {
        return jdbcTemplate.query(
                "SELECT code, id FROM countries",
                rs -> {
                    Map<String, Long> result = new java.util.HashMap<>();
                    while (rs.next()) {
                        result.put(rs.getString("code"), rs.getLong("id"));
                    }
                    return result;
                });
    }

    public Map<String, Long> findStatusIdsByCode() {
        return jdbcTemplate.query(
                "SELECT code, id FROM customer_status",
                rs -> {
                    Map<String, Long> result = new java.util.HashMap<>();
                    while (rs.next()) {
                        result.put(rs.getString("code"), rs.getLong("id"));
                    }
                    return result;
                });
    }

    public Set<String> findExistingExternalIds(Collection<String> externalIds) {
        if (externalIds.isEmpty()) {
            return Set.of();
        }

        String sql = "SELECT external_id FROM customers WHERE external_id IN (:externalIds)";
        return namedParameterJdbcTemplate.query(
                sql,
                new MapSqlParameterSource("externalIds", externalIds),
                (rs, rowNum) -> rs.getString("external_id"))
                .stream()
                .collect(Collectors.toSet());
    }

    public int bulkInsertCustomers(List<ResolvedCustomer> customers) {
        if (customers.isEmpty()) {
            return 0;
        }

        String sql = """
                INSERT INTO customers (external_id, name, email, country_id, status_id)
                VALUES (?, ?, ?, ?, ?)
                ON CONFLICT (external_id) DO NOTHING
                """;

        List<Object[]> batchArgs = customers.stream()
                .map(customer -> new Object[]{
                        customer.externalId(),
                        customer.name(),
                        customer.email(),
                        customer.countryId(),
                        customer.statusId()
                })
                .toList();

        int[] counts = jdbcTemplate.batchUpdate(sql, batchArgs);

        int inserted = 0;
        for (int count : counts) {
            if (count > 0 || count == Statement.SUCCESS_NO_INFO) {
                inserted++;
            }
        }
        return inserted;
    }
}
