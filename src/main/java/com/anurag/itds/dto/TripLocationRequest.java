package com.anurag.itds.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * {@code POST /api/trips/{id}/location} — the user explicitly shares a location
 * while a trip is active. SafeWalk only stores this single consented snapshot.
 */
public record TripLocationRequest(
        @NotNull(message = "Latitude is required.")
        @Min(value = -90, message = "Latitude must be between -90 and 90.")
        @Max(value = 90, message = "Latitude must be between -90 and 90.")
        Double latitude,

        @NotNull(message = "Longitude is required.")
        @Min(value = -180, message = "Longitude must be between -180 and 180.")
        @Max(value = 180, message = "Longitude must be between -180 and 180.")
        Double longitude
) {
}
