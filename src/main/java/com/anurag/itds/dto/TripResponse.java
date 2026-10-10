package com.anurag.itds.dto;

import com.anurag.itds.entity.TripStatus;

import java.time.Instant;

/** A trip as returned to the client. */
public record TripResponse(
        Long id,
        TripStatus status,
        String originLabel,
        Double originLatitude,
        Double originLongitude,
        String destinationLabel,
        Double destinationLatitude,
        Double destinationLongitude,
        Double lastLatitude,
        Double lastLongitude,
        Instant startedAt,
        Instant endedAt,
        Instant createdAt,
        Instant updatedAt
) {
}
