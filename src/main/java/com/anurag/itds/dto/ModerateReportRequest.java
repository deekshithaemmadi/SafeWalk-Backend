package com.anurag.itds.dto;

import com.anurag.itds.entity.ReportStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * {@code POST /api/community-reports/{id}/moderate} — moderator sets a report's
 * status to VERIFIED or REJECTED. Restricted to the MODERATOR role in
 * {@code SecurityConfig}.
 */
public record ModerateReportRequest(
        @NotNull(message = "Status is required.")
        ReportStatus status,

        @Size(max = 500, message = "Moderation note must be at most 500 characters.")
        String note
) {
}
