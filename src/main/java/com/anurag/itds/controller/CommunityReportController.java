package com.anurag.itds.controller;

import com.anurag.itds.dto.CommunityReportResponse;
import com.anurag.itds.dto.CreateReportRequest;
import com.anurag.itds.dto.ModerateReportRequest;
import com.anurag.itds.dto.UpdateReportRequest;
import com.anurag.itds.entity.ReportStatus;
import com.anurag.itds.exception.BadRequestException;
import com.anurag.itds.security.CurrentUserProvider;
import com.anurag.itds.service.CommunityReportService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Community safety reports. Listing is scoped to permitted data (own reports +
 * VERIFIED reports); moderation is restricted to the MODERATOR role in
 * {@code SecurityConfig}.
 */
@RestController
@RequestMapping("/api/community-reports")
public class CommunityReportController {

    private final CommunityReportService reportService;
    private final CurrentUserProvider currentUserProvider;

    public CommunityReportController(CommunityReportService reportService,
                                     CurrentUserProvider currentUserProvider) {
        this.reportService = reportService;
        this.currentUserProvider = currentUserProvider;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CommunityReportResponse create(@Valid @RequestBody CreateReportRequest request) {
        return reportService.create(currentUserProvider.requireCurrentUserId(), request);
    }

    @GetMapping
    public Page<CommunityReportResponse> list(@RequestParam(required = false) String status,
                                              @RequestParam(required = false) String category,
                                              Pageable pageable) {
        ReportStatus statusFilter = null;
        if (status != null && !status.isBlank()) {
            try {
                statusFilter = ReportStatus.valueOf(status.trim().toUpperCase());
            } catch (IllegalArgumentException ex) {
                throw new BadRequestException(
                        "Invalid status filter. Allowed: PENDING, VERIFIED, REJECTED.");
            }
        }
        return reportService.listPermitted(
                currentUserProvider.requireCurrentUserId(), statusFilter, category, pageable);
    }

    @GetMapping("/{id}")
    public CommunityReportResponse get(@PathVariable Long id) {
        return reportService.getVisible(currentUserProvider.requireCurrentUserId(), id);
    }

    @PutMapping("/{id}")
    public CommunityReportResponse update(@PathVariable Long id,
                                          @Valid @RequestBody UpdateReportRequest request) {
        return reportService.updateOwn(currentUserProvider.requireCurrentUserId(), id, request);
    }

    @PostMapping("/{id}/moderate")
    public CommunityReportResponse moderate(@PathVariable Long id,
                                            @Valid @RequestBody ModerateReportRequest request) {
        // Restricted to MODERATOR role by SecurityConfig.
        return reportService.moderate(id, request);
    }
}
