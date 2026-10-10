package com.anurag.itds.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * {@code POST /api/sos} request body, matching the frontend {@code preparedPayload}.
 *
 * <p>Location is only persisted when {@code shareLocation} is {@code true} AND a
 * location is supplied — the user's explicit consent. SafeWalk records that SOS was
 * activated but never automatically dispatches/calls/SMS/broadcasts an alert.</p>
 *
 * @param message        optional user message
 * @param shareLocation  whether the user consented to share their location
 * @param latitude       shared latitude (ignored unless shareLocation is true)
 * @param longitude      shared longitude (ignored unless shareLocation is true)
 * @param accuracy       optional accuracy in metres
 * @param requestedAt    optional client timestamp (ISO-8601); server always records its own too
 */
public record SosRequest(
        @Size(max = 500, message = "Message must be at most 500 characters.")
        String message,

        @NotNull(message = "shareLocation flag is required.")
        Boolean shareLocation,

        @Min(value = -90, message = "Latitude must be between -90 and 90.")
        @Max(value = 90, message = "Latitude must be between -90 and 90.")
        Double latitude,

        @Min(value = -180, message = "Longitude must be between -180 and 180.")
        @Max(value = 180, message = "Longitude must be between -180 and 180.")
        Double longitude,

        @Min(value = 0, message = "Accuracy must be a positive number.")
        Double accuracy,

        String requestedAt
) {
}
