package com.kieumai.notifier.presentation.controller;

import com.kieumai.notifier.business.exception.ReportException;
import com.kieumai.notifier.business.model.Report;
import com.kieumai.notifier.business.model.ReportUpdate;
import com.kieumai.notifier.business.service.*;
import com.kieumai.notifier.presentation.dto.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class ReportController {
    private final ReportService reportService;
    private final AuthService authService;

    @PostMapping("/reports")
    @ResponseStatus(HttpStatus.CREATED)
    public ReportDto createReport(@Valid @RequestBody CreateReportRequest request, Authentication authentication) {
        Report report = new Report();
        report.setUserId(currentUserId(authentication));
        report.setWardId(request.getWardId());
        report.setType(request.getType());
        report.setDescription(request.getDescription());
        report.setLatitude(request.getLatitude());
        report.setLongitude(request.getLongitude());
        report.setObservedAt(request.getObservedAt());
        return toDto(reportService.createReport(report));
    }

    @GetMapping("/reports")
    public PageResponse<ReportDto> getReports(
            @RequestParam(required = false) Long wardId,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) Long incidentId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return PageResponse.from(reportService.getReports(wardId, type, incidentId, null, page, size).map(this::toDto));
    }

    @GetMapping("/me/reports")
    public PageResponse<ReportDto> getMyReports(
            @RequestParam(required = false) Long wardId,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) Long incidentId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication) {
        return PageResponse.from(reportService.getReports(wardId, type, incidentId,
                currentUserId(authentication), page, size).map(this::toDto));
    }

    @DeleteMapping("/reports/{reportId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteReport(@PathVariable long reportId, Authentication authentication) {
        reportService.deleteReport(reportId, currentUserId(authentication));
    }

    @PostMapping("/reports/{reportId}/updates")
    @ResponseStatus(HttpStatus.CREATED)
    public ReportUpdateDto createReportUpdate(
            @PathVariable long reportId,
            @Valid @RequestBody CreateReportUpdateRequest request,
            Authentication authentication) {
        return toUpdateDto(reportService.createReportUpdate(
                reportId, currentUserId(authentication), request.getContent()));
    }

    @GetMapping("/reports/{reportId}/updates")
    public List<ReportUpdateDto> getReportUpdates(@PathVariable long reportId) {
        return reportService.getReportUpdates(reportId).stream()
                .map(this::toUpdateDto)
                .toList();
    }

    private Long currentUserId(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ReportException(ReportException.Code.UNAUTHORIZED, "Authentication is required.");
        }
        var user = authService.findByUsername(authentication.getName());
        if (user == null) throw new ReportException(ReportException.Code.UNAUTHORIZED, "User does not exist.");
        return user.getId();
    }

    private ReportDto toDto(Report report) {
        ReportDto response = new ReportDto();
        response.setId(report.getId());
        response.setUserId(report.getUserId());
        response.setIncidentId(report.getIncidentId());
        response.setWardId(report.getWardId());
        response.setType(report.getType());
        response.setDescription(report.getDescription());
        response.setLatitude(report.getLatitude());
        response.setLongitude(report.getLongitude());
        response.setObservedAt(report.getObservedAt());
        response.setCreatedAt(report.getCreatedAt());
        return response;
    }

    private ReportUpdateDto toUpdateDto(ReportUpdate update) {
        ReportUpdateDto response = new ReportUpdateDto();
        response.setId(update.getId());
        response.setReportId(update.getReportId());
        response.setUserId(update.getUserId());
        response.setContent(update.getContent());
        response.setCreatedAt(update.getCreatedAt());
        return response;
    }
}
