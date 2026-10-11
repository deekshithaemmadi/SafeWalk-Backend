package com.anurag.itds.service;

import com.anurag.itds.dto.HelpLocationMetaResponse;
import com.anurag.itds.dto.HelpLocationResponse;
import com.anurag.itds.dto.NearbyHelpLocationResponse;
import com.anurag.itds.entity.HelpLocation;
import com.anurag.itds.entity.HelpLocationType;
import com.anurag.itds.exception.BadRequestException;
import com.anurag.itds.exception.ResourceNotFoundException;
import com.anurag.itds.repository.HelpLocationRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Read-only directory of help locations (hospitals, police stations, women's
 * help centres).
 *
 * <p><strong>Data honesty:</strong> locations are served only from what has
 * actually been imported into the {@code help_locations} table. No coordinates
 * or venues are fabricated, and records without real source coordinates expose
 * {@code hasCoordinates=false} and are excluded from distance queries. Provenance
 * fields ({@code source}, {@code sourceYear}) are preserved and returned.</p>
 */
@Service
public class HelpLocationService {

    static final double MAX_RADIUS_KM = 50.0;
    static final int MAX_NEARBY_LIMIT = 100;
    private static final double EARTH_RADIUS_KM = 6371.0088;

    static final List<String> LIMITATIONS = List.of(
            "Records come from static, imported datasets; they are not live and are not verified in real time.",
            "Phone numbers, addresses and opening hours may be outdated. Call 112 (or 100 police / 181 women helpline) in an emergency.",
            "Records without source coordinates are listed but excluded from nearby/distance results.",
            "Distances are straight-line (haversine) estimates, not travel distance or time.",
            "No crime statistics, live incident data or safety scores are provided by this API."
    );

    private final HelpLocationRepository helpLocationRepository;

    public HelpLocationService(HelpLocationRepository helpLocationRepository) {
        this.helpLocationRepository = helpLocationRepository;
    }

    @Transactional(readOnly = true)
    public Page<HelpLocationResponse> search(HelpLocationType type, String q,
                                             String district, Pageable pageable) {
        return helpLocationRepository.search(type, blankToNull(q), blankToNull(district), pageable)
                .map(Dtos::toHelpLocationResponse);
    }

    @Transactional(readOnly = true)
    public HelpLocationResponse get(Long id) {
        return helpLocationRepository.findById(id)
                .map(Dtos::toHelpLocationResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Help location not found."));
    }

    /** Locations with real coordinates within {@code radiusKm} of the point, nearest first. */
    @Transactional(readOnly = true)
    public List<NearbyHelpLocationResponse> nearby(double lat, double lon, double radiusKm,
                                                   HelpLocationType type, int limit) {
        if (!Double.isFinite(lat) || lat < -90 || lat > 90) {
            throw new BadRequestException("'lat' must be a latitude between -90 and 90.");
        }
        if (!Double.isFinite(lon) || lon < -180 || lon > 180) {
            throw new BadRequestException("'lon' must be a longitude between -180 and 180.");
        }
        if (!Double.isFinite(radiusKm) || radiusKm <= 0 || radiusKm > MAX_RADIUS_KM) {
            throw new BadRequestException("'radiusKm' must be greater than 0 and at most " + (int) MAX_RADIUS_KM + ".");
        }
        if (limit < 1 || limit > MAX_NEARBY_LIMIT) {
            throw new BadRequestException("'limit' must be between 1 and " + MAX_NEARBY_LIMIT + ".");
        }
        return helpLocationRepository.findByLatitudeIsNotNullAndLongitudeIsNotNull().stream()
                .filter(h -> type == null || h.getType() == type)
                .map(h -> new NearbyHelpLocationResponse(Dtos.toHelpLocationResponse(h),
                        round(haversineKm(lat, lon, h.getLatitude(), h.getLongitude()))))
                .filter(n -> n.distanceKm() <= radiusKm)
                .sorted(Comparator.comparingDouble(NearbyHelpLocationResponse::distanceKm))
                .limit(limit)
                .toList();
    }

    @Transactional(readOnly = true)
    public HelpLocationMetaResponse meta() {
        Map<String, Long> counts = new LinkedHashMap<>();
        for (HelpLocationType t : HelpLocationType.values()) {
            counts.put(t.name(), helpLocationRepository.countByType(t));
        }
        long withCoords = helpLocationRepository.findByLatitudeIsNotNullAndLongitudeIsNotNull().size();
        return new HelpLocationMetaResponse(
                helpLocationRepository.count(),
                counts,
                withCoords,
                helpLocationRepository.findDistinctSources(),
                "STATIC_DATASET",
                false,
                LIMITATIONS);
    }

    /**
     * Parses the {@code type} query parameter. A blank value means "all types";
     * an unparsable value is a 400 (not a silent 500).
     */
    public HelpLocationType parseType(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return HelpLocationType.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException(
                    "Invalid type filter. Allowed: HOSPITAL, POLICE, WOMEN_HELP.");
        }
    }

    static double haversineKm(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return 2 * EARTH_RADIUS_KM * Math.asin(Math.min(1.0, Math.sqrt(a)));
    }

    private static double round(double km) {
        return Math.round(km * 100.0) / 100.0;
    }

    private String blankToNull(String s) {
        return (s == null || s.isBlank()) ? null : s.trim();
    }
}
