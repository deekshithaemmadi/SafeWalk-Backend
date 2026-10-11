package com.anurag.itds.dto;

/**
 * A help location plus its straight-line (haversine) distance from the query point.
 * This is NOT a travel distance or travel time.
 */
public record NearbyHelpLocationResponse(
        HelpLocationResponse location,
        double distanceKm
) {
}
