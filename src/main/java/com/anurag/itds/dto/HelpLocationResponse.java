package com.anurag.itds.dto;

import com.anurag.itds.entity.HelpLocationType;

/**
 * A help location as returned to the client.
 *
 * <p>{@code latitude}/{@code longitude} are {@code null} for police stations and
 * women's help centres because those source datasets contain no coordinates. The
 * {@code hasCoordinates} flag lets the UI plot only the records that truly have
 * real coordinates.</p>
 */
public record HelpLocationResponse(
        Long id,
        HelpLocationType type,
        String name,
        String category,
        String district,
        String locality,
        String address,
        Double latitude,
        Double longitude,
        String phone,
        String supportSectors,
        String tier,
        String sourceYear,
        String source,
        boolean hasCoordinates
) {
}
