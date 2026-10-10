package com.anurag.itds.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;

/**
 * {@code POST /api/trips} request body — create a planned trip.
 * Origin/destination are optional (a user may plan later), but when supplied are
 * validated.
 */
public record CreateTripRequest(
        @Size(max = 255, message = "Origin label must be at most 255 characters.")
        String originLabel,

        @Valid
        GeoPointRequest origin,

        @Size(max = 255, message = "Destination label must be at most 255 characters.")
        String destinationLabel,

        @Valid
        GeoPointRequest destination
) {
}
