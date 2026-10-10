package com.anurag.itds.dto;

import java.time.Instant;
import java.util.Map;

/**
 * Uniform error body returned for every failed API request.
 *
 * <p>The frontend {@code api.js} reads {@code message} (or {@code error}) to show
 * to the user, so {@link #message} is always populated.</p>
 *
 * @param timestamp when the error occurred (ISO-8601)
 * @param status    HTTP status code
 * @param error     short reason phrase (e.g. "Bad Request")
 * @param message   human-readable message
 * @param path      the request path that failed
 * @param fieldErrors optional per-field validation messages (may be null)
 */
public record ErrorResponse(
        Instant timestamp,
        int status,
        String error,
        String message,
        String path,
        Map<String, String> fieldErrors
) {
}
