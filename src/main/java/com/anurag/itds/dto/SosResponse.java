package com.anurag.itds.dto;

import java.time.Instant;

/**
 * {@code POST /api/sos} response.
 *
 * <p>Critically, {@code delivered} is always {@code false}: SafeWalk does NOT send,
 * dispatch, or deliver alerts. It only records that SOS was activated. The response
 * makes this explicit so the UI never misleads the user.</p>
 *
 * @param id        the created SOS event id
 * @param delivered always false (no automated delivery happens)
 * @param message   human-readable confirmation / disclaimer
 * @param createdAt when the event was recorded
 */
public record SosResponse(
        Long id,
        boolean delivered,
        String message,
        Instant createdAt
) {
}
