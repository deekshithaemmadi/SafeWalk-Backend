package com.anurag.itds.repository;

import com.anurag.itds.entity.HelpLocation;
import com.anurag.itds.entity.HelpLocationType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface HelpLocationRepository extends JpaRepository<HelpLocation, Long> {

    Optional<HelpLocation> findBySourceKey(String sourceKey);

    Page<HelpLocation> findByType(HelpLocationType type, Pageable pageable);

    Page<HelpLocation> findByDistrict(String district, Pageable pageable);

    Page<HelpLocation> findByTypeAndDistrict(HelpLocationType type, String district, Pageable pageable);

    /**
     * Case-insensitive substring search across name, locality and address.
     * Used for the {@code q} filter of {@code GET /api/help-locations}.
     */
    @Query("""
            SELECT h FROM HelpLocation h
            WHERE (:type IS NULL OR h.type = :type)
              AND (
                    :q IS NULL
                    OR LOWER(h.name) LIKE LOWER(CONCAT('%', :q, '%'))
                    OR LOWER(COALESCE(h.locality, '')) LIKE LOWER(CONCAT('%', :q, '%'))
                    OR LOWER(COALESCE(h.address, '')) LIKE LOWER(CONCAT('%', :q, '%'))
                  )
            """)
    Page<HelpLocation> search(@Param("type") HelpLocationType type,
                              @Param("q") String q,
                              Pageable pageable);
}
