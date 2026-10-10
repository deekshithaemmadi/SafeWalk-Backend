package com.anurag.itds.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/** A geographic point in a trip request (latitude/longitude). */
public record GeoPointRequest(
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
