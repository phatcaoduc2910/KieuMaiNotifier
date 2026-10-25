package com.kieumai.notifier.presentation.controller;

import com.kieumai.notifier.business.exception.ReportException;
import com.kieumai.notifier.business.model.Report;
import com.kieumai.notifier.business.service.AuthService;
import com.kieumai.notifier.business.service.ReportService;
import com.kieumai.notifier.presentation.dto.CreateReportRequest;
import com.kieumai.notifier.presentation.dto.PageResponse;
import com.kieumai.notifier.presentation.dto.ReportDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

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
        report.setLocationDescription(request.getLocationDescription());
        report.setImageUrls(request.getImageUrls());
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
        response.setLocationDescription(report.getLocationDescription());
        response.setImageUrls(report.getImageUrls());
        response.setLatitude(report.getLatitude());
        response.setLongitude(report.getLongitude());
        response.setObservedAt(report.getObservedAt());
        response.setCreatedAt(report.getCreatedAt());
        return response;
    }
}
