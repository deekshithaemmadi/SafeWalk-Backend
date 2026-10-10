package com.anurag.itds.dto;

import com.anurag.itds.entity.ReportStatus;

import java.time.Instant;

/** A community report as returned to the client. */
public record CommunityReportResponse(
        Long id,
        Long userId,
        String category,
        String description,
        String location,
        Double latitude,
        Double longitude,
        ReportStatus status,
        String moderationNote,
        Instant createdAt,
        Instant updatedAt
) {
}
