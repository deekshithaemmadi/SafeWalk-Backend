package com.anurag.itds.service;

import com.anurag.itds.dto.HelpLocationResponse;
import com.anurag.itds.entity.HelpLocation;
import com.anurag.itds.entity.HelpLocationType;
import com.anurag.itds.exception.BadRequestException;
import com.anurag.itds.repository.HelpLocationRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Read-only directory of help locations (hospitals, police stations, women's
 * help centres).
 *
 * <p><strong>Data honesty:</strong> locations are served only from what has
 * actually been imported into the {@code help_locations} table. No coordinates
 * or venues are fabricated, and records without real source coordinates expose
 * {@code hasCoordinates=false}. Provenance fields ({@code source},
 * {@code sourceYear}) are preserved and returned.</p>
 */
@Service
public class HelpLocationService {

    private final HelpLocationRepository helpLocationRepository;

    public HelpLocationService(HelpLocationRepository helpLocationRepository) {
        this.helpLocationRepository = helpLocationRepository;
    }

    @Transactional(readOnly = true)
    public Page<HelpLocationResponse> search(HelpLocationType type, String q,
                                             String district, Pageable pageable) {
        if (type == null && (district == null || district.isBlank())) {
            return helpLocationRepository.search(null, blankToNull(q), pageable)
                    .map(Dtos::toHelpLocationResponse);
        }
        if (type != null && district != null && !district.isBlank()) {
            return helpLocationRepository.findByTypeAndDistrict(type, district.trim(), pageable)
                    .map(Dtos::toHelpLocationResponse);
        }
        return helpLocationRepository.search(type, blankToNull(q), pageable)
                .map(Dtos::toHelpLocationResponse);
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

    private String blankToNull(String s) {
        return (s == null || s.isBlank()) ? null : s.trim();
    }
}
