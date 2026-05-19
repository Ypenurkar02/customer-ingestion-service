# Customer Ingestion Service

Production-oriented Spring Boot service for scalable customer delta ingestion with lookup resolution, idempotency, chunk processing, and bulk PostgreSQL writes.

**Submitting to an employer:** use a Git repo whose root is this folder only (not your home directory). See [docs/SUBMISSION.md](docs/SUBMISSION.md) for GitHub + Render steps and a safe `git init` workflow.

## Assignment Focus

This is not a CRUD application. The service is optimized for ingesting large customer payloads, resolving reference data once per request, comparing incoming records against existing customers in bulk, and inserting only new records.

## Tech Stack

- Java 17
- Spring Boot 3
- PostgreSQL
- Maven
- Flyway migrations
- JdbcTemplate for bulk database operations
- Docker and Docker Compose
- JUnit 5 and Testcontainers
- Springdoc OpenAPI

## Architecture

```text
POST /customers/ingest
        |
        v
CustomerIngestionController
        |
        v
CustomerIngestionService
        |
        v
BatchProcessor
   |          |
   v          v
LookupResolver  DeltaDetector
        |          |
        v          v
CustomerJdbcRepository
        |
        v
PostgreSQL
```

Important components:

- `LookupResolver` loads `countries` and `customer_status` into memory maps once per request.
- `DeltaDetector` fetches existing `external_id` values with bulk `IN` queries.
- `BatchProcessor` validates records, handles duplicates, resolves lookups, and prepares insert candidates.
- `CustomerJdbcRepository` uses `JdbcTemplate` batch operations and PostgreSQL `ON CONFLICT DO NOTHING`.
- `CustomerIngestionService` processes configurable chunks with chunk-level transaction boundaries.

## Design Decisions

`JdbcTemplate` is used for ingestion paths because batch processing needs predictable SQL, efficient bulk reads, and bulk inserts. JPA is useful for entity-centric workflows, but `repository.save()` loops would create unnecessary round trips and make it easier to accidentally implement N+1 behavior.

Delta detection is done by collecting valid incoming `external_id` values for each chunk, querying PostgreSQL once for matching records, and computing the difference with an in-memory `HashSet`.

Idempotency is enforced in two layers:

- Application layer: existing `external_id` values are skipped before insert.
- Database layer: `customers.external_id` is unique and inserts use `ON CONFLICT (external_id) DO NOTHING`.

## Database Schema

Flyway creates and seeds:

- `countries`: `US`, `IN`, `UK`
- `customer_status`: `ACTIVE`, `INACTIVE`
- `customers`: unique `external_id`, optional name/email, foreign keys to lookup tables, and indexes for lookup and delta operations

Migration file: `src/main/resources/db/migration/V1__create_customer_ingestion_schema.sql`

## Running Locally

Prerequisites:

- Java 17
- Docker
- Maven wrapper from this repository

Start PostgreSQL and the app:

```bash
docker compose up --build
```

The API will be available at:

- App: `http://localhost:8080`
- Health: `http://localhost:8080/actuator/health`
- Swagger UI: `http://localhost:8080/swagger-ui.html`

Run tests:

```bash
./mvnw test
```

Testcontainers tests are configured to skip when Docker is unavailable.

## API Usage

Endpoint:

```http
POST /customers/ingest
Content-Type: application/json
```

Request:

```json
[
  {
    "external_id": "cust_001",
    "name": "Alice",
    "email": "alice@example.com",
    "country_code": "US",
    "status_code": "ACTIVE"
  },
  {
    "external_id": "cust_002",
    "name": "Bob",
    "email": "bob@example.com",
    "country_code": "IN",
    "status_code": "INACTIVE"
  }
]
```

Example:

```bash
curl -X POST http://localhost:8080/customers/ingest \
  -H "Content-Type: application/json" \
  -d @sample-requests/customers-ingest.json
```

Response:

```json
{
  "received": 2,
  "inserted": 2,
  "skipped_existing": 0,
  "failed": 0,
  "duration_ms": 42,
  "dry_run": false,
  "duplicate_external_ids": [],
  "failed_records": [],
  "metrics": {
    "chunks_processed": 1,
    "rows_scanned": 2,
    "rows_inserted": 2,
    "cache_hits": 4
  }
}
```

Dry run:

```bash
curl -X POST "http://localhost:8080/customers/ingest?dryRun=true" \
  -H "Content-Type: application/json" \
  -d @sample-requests/customers-ingest.json
```

## Validation Behavior

The service processes valid records even when some records fail validation. Failed records are returned with their input index and reason.

Handled validation cases:

- Missing or blank `external_id`
- Duplicate `external_id` within the same payload
- Invalid `country_code`
- Invalid `status_code`
- Existing customers already present in the database

## Configuration

Environment variables:

```text
DATABASE_HOST=localhost
DATABASE_PORT=5432
DATABASE_NAME=customer_ingestion
DATABASE_USERNAME=customer_ingestion
DATABASE_PASSWORD=customer_ingestion
INGESTION_CHUNK_SIZE=1000
```

The default chunk size is `1000`, which keeps transactions bounded while still allowing large requests such as 100k records to be processed with low database round trips.

## Render Deployment

This repository includes `render.yaml` for a Docker-based Render deployment with a managed PostgreSQL database.

Recommended steps:

1. Push this repository to GitHub.
2. In Render, create a new Blueprint from the repository.
3. Render will provision the web service and PostgreSQL database from `render.yaml`.
4. After deployment, verify `https://<your-service>.onrender.com/actuator/health`.
5. Use `https://<your-service>.onrender.com/swagger-ui.html` to interact with the API.

## Performance Notes

- Lookup tables are loaded once per request into maps for O(1) lookup resolution.
- Existing customers are fetched in bulk per chunk, avoiding row-by-row checks.
- Inserts are batched and protected by `ON CONFLICT DO NOTHING`.
- Transactions are scoped to chunks rather than the entire request.
- The ingestion path avoids repository save loops and N+1 database access patterns.

## Future Improvements

- Add authentication and request rate limiting.
- Persist ingestion job history for audit and replay.
- Add Micrometer business metrics for skipped, failed, and inserted records.
- Support asynchronous ingestion for very large files.
- Add CSV upload support backed by the same ingestion pipeline.
