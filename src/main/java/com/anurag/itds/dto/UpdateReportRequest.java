package com.anurag.itds.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;

/**
 * {@code PUT /api/community-reports/{id}} — the owner updates their own report.
 *
 * <p>The owner may edit content only. They may NOT set the moderation status
 * (that is reserved for the {@code /moderate} endpoint and moderators). A new
 * report always starts {@code PENDING}.</p>
 */
public record UpdateReportRequest(
        @Size(max = 120, message = "Category must be at most 120 characters.")
        String category,

        @Size(min = 10, max = 1000, message = "Description must be between 10 and 1000 characters.")
        String description,

        @Size(max = 255, message = "Location must be at most 255 characters.")
        String location,

        @DecimalMin(value = "-90.0", message = "Latitude must be between -90 and 90.")
        @DecimalMax(value = "90.0", message = "Latitude must be between -90 and 90.")
        Double latitude,

        @DecimalMin(value = "-180.0", message = "Longitude must be between -180 and 180.")
        @DecimalMax(value = "180.0", message = "Longitude must be between -180 and 180.")
        Double longitude
) {
}
