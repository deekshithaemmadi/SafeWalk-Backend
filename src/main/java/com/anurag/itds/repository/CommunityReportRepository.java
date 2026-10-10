package com.anurag.itds.repository;

import com.anurag.itds.entity.CommunityReport;
import com.anurag.itds.entity.ReportStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface CommunityReportRepository extends JpaRepository<CommunityReport, Long> {

    Optional<CommunityReport> findByIdAndUserId(Long id, Long userId);

    Page<CommunityReport> findByStatus(ReportStatus status, Pageable pageable);

    /**
     * Filtered, paginated listing with an optional case-insensitive category
     * substring match. Used by {@code GET /api/community-reports}.
     */
    @Query("""
            SELECT r FROM CommunityReport r
            WHERE (:status IS NULL OR r.status = :status)
              AND (
                    :category IS NULL
                    OR LOWER(r.category) LIKE LOWER(CONCAT('%', :category, '%'))
                  )
            """)
    Page<CommunityReport> filter(@Param("status") ReportStatus status,
                                 @Param("category") String category,
                                 Pageable pageable);

    /**
     * Paginated listing of the reports a user is allowed to see: their own
     * reports (any status) plus every VERIFIED report. PENDING/REJECTED reports
     * from other users are never visible. Used by
     * {@code GET /api/community-reports}.
     */
    @Query("""
            SELECT r FROM CommunityReport r
            WHERE (r.user.id = :userId OR r.status = :publicStatus)
              AND (
                    :category IS NULL
                    OR LOWER(r.category) LIKE LOWER(CONCAT('%', :category, '%'))
                  )
            """)
    Page<CommunityReport> findPermitted(@Param("userId") Long userId,
                                        @Param("publicStatus") ReportStatus publicStatus,
                                        @Param("category") String category,
                                        Pageable pageable);
}
