package com.anurag.itds.dto;

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

        Double latitude,
        Double longitude
) {
}
