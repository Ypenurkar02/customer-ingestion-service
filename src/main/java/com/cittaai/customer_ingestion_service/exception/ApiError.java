package com.cittaai.customer_ingestion_service.exception;

import java.time.Instant;

public record ApiError(Instant timestamp, int status, String error, String message) {
}
