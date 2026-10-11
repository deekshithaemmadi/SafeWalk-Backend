package com.anurag.itds.service;

import com.anurag.itds.dto.CommunityReportResponse;
import com.anurag.itds.dto.CreateReportRequest;
import com.anurag.itds.dto.ModerateReportRequest;
import com.anurag.itds.dto.UpdateReportRequest;
import com.anurag.itds.entity.CommunityReport;
import com.anurag.itds.entity.ReportStatus;
import com.anurag.itds.entity.User;
import com.anurag.itds.exception.BadRequestException;
import com.anurag.itds.exception.ForbiddenException;
import com.anurag.itds.exception.ResourceNotFoundException;
import com.anurag.itds.repository.CommunityReportRepository;
import com.anurag.itds.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Community safety reports.
 *
 * <p>Visibility rules ("permitted report data"):
 * <ul>
 *   <li>Everyone authenticated sees all VERIFIED reports plus their own.</li>
 *   <li>PENDING/REJECTED reports are visible only to their owner.</li>
 *   <li>Only the owner may edit a report, and only its content — never its
 *       moderation status.</li>
 *   <li>Only moderators (enforced by {@code SecurityConfig}) may change a
 *       report's status via {@link #moderate}.</li>
 * </ul>
 * A pending report is never presented as verified.</p>
 */
@Service
public class CommunityReportService {

    private final CommunityReportRepository reportRepository;
    private final UserRepository userRepository;

    public CommunityReportService(CommunityReportRepository reportRepository,
                                  UserRepository userRepository) {
        this.reportRepository = reportRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public CommunityReportResponse create(Long userId, CreateReportRequest request) {
        User user = requireUser(userId);
        CommunityReport saved = reportRepository.save(new CommunityReport(
                user,
                request.category().trim(),
                request.description().trim()));
        saved.setLocation(request.location());
        saved.setLatitude(request.latitude());
        saved.setLongitude(request.longitude());
        return Dtos.toReportResponse(reportRepository.save(saved));
    }

    @Transactional(readOnly = true)
    public Page<CommunityReportResponse> listPermitted(Long userId, ReportStatus status,
                                                       String category, Pageable pageable) {
        return reportRepository.findPermitted(userId, ReportStatus.VERIFIED, status, category, pageable)
                .map(Dtos::toReportResponse);
    }

    @Transactional(readOnly = true)
    public CommunityReportResponse getVisible(Long userId, Long reportId) {
        CommunityReport report = requireReport(reportId);
        if (!isVisibleTo(report, userId)) {
            // Hide existence from non-owners.
            throw new ResourceNotFoundException("Report not found.");
        }
        return Dtos.toReportResponse(report);
    }

    @Transactional
    public CommunityReportResponse updateOwn(Long userId, Long reportId, UpdateReportRequest request) {
        CommunityReport report = requireOwnedReport(userId, reportId);

        boolean hasChange = false;
        if (request.category() != null && !request.category().isBlank()) {
            report.setCategory(request.category().trim());
            hasChange = true;
        }
        if (request.description() != null && !request.description().isBlank()) {
            report.setDescription(request.description().trim());
            hasChange = true;
        }
        if (request.location() != null) {
            report.setLocation(request.location().isBlank() ? null : request.location().trim());
            hasChange = true;
        }
        if (request.latitude() != null || request.longitude() != null) {
            report.setLatitude(request.latitude());
            report.setLongitude(request.longitude());
            hasChange = true;
        }
        if (!hasChange) {
            throw new BadRequestException("No updatable fields were provided.");
        }
        return Dtos.toReportResponse(reportRepository.save(report));
    }

    @Transactional
    public CommunityReportResponse moderate(Long reportId, ModerateReportRequest request) {
        if (request.status() == ReportStatus.PENDING) {
            throw new BadRequestException("A report can only be moderated to VERIFIED or REJECTED.");
        }
        CommunityReport report = requireReport(reportId);
        report.setStatus(request.status());
        report.setModerationNote(request.note());
        return Dtos.toReportResponse(reportRepository.save(report));
    }

    private boolean isVisibleTo(CommunityReport report, Long userId) {
        return report.getStatus() == ReportStatus.VERIFIED
                || (report.getUser() != null && report.getUser().getId().equals(userId));
    }

    private CommunityReport requireOwnedReport(Long userId, Long reportId) {
        CommunityReport report = requireReport(reportId);
        if (report.getUser() == null || !report.getUser().getId().equals(userId)) {
            throw new ForbiddenException("You can only modify your own reports.");
        }
        return report;
    }

    private CommunityReport requireReport(Long reportId) {
        return reportRepository.findById(reportId)
                .orElseThrow(() -> new ResourceNotFoundException("Report not found."));
    }

    private User requireUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found."));
    }
}
