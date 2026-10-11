package com.anurag.itds.dto;

import java.util.List;
import java.util.Map;

/**
 * Describes what the help-location directory actually contains, where it came from
 * and what it cannot tell the user. {@code live} is always false: records are
 * imported from static datasets and are not verified in real time.
 */
public record HelpLocationMetaResponse(
        long totalRecords,
        Map<String, Long> countsByType,
        long recordsWithCoordinates,
        List<String> sources,
        String dataNature,
        boolean live,
        List<String> limitations
) {
}
