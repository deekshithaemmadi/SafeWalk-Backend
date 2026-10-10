package com.anurag.itds.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * {@code POST /api/community-reports} request body.
 * Mirrors the frontend report shape: a category, a description, and an optional
 * location (free text or "lat, lon").
 */
public record CreateReportRequest(
        @NotBlank(message = "Please choose a category.")
        @Size(max = 120, message = "Category must be at most 120 characters.")
        String category,

        @NotBlank(message = "Please describe the safety concern.")
        @Size(min = 10, max = 1000, message = "Description must be between 10 and 1000 characters.")
        String description,

        @Size(max = 255, message = "Location must be at most 255 characters.")
        String location,

        Double latitude,
        Double longitude
) {
}
