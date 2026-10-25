package com.kieumai.notifier.business.service;

import com.kieumai.notifier.business.exception.ReportException;
import com.kieumai.notifier.business.model.Report;
import com.kieumai.notifier.persistence.entity.ReportEntity;
import com.kieumai.notifier.persistence.repository.IncidentRepository;
import com.kieumai.notifier.persistence.repository.ReportRepository;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Set;

import static com.kieumai.notifier.business.exception.ReportException.Code.*;

@Service
@RequiredArgsConstructor
public class ReportService {
    private static final Set<String> TYPES = Set.of("FLOOD", "TRAFFIC_JAM", "OTHER");

    private final ReportRepository reportRepository;
    private final IncidentRepository incidentRepository;

    public Report createReport(Report report) {
        // TODO: Connect report creation after the incident module exposes its integration contract.
        throw new ReportException(INCIDENT_INTEGRATION_PENDING,
                "Report creation is pending integration with the incident module.");
    }

    @Transactional(readOnly = true)
    public Page<Report> getReports(Long wardId, String type, Long incidentId, Long userId, int page, int size) {
        if (page < 0 || size < 1 || size > 100 || (wardId != null && wardId < 1)
                || (incidentId != null && incidentId < 1) || (type != null && !TYPES.contains(type))) {
            throw new ReportException(VALIDATION_ERROR, "Invalid report filter or pagination.");
        }
        if (incidentId != null && !incidentRepository.existsById(incidentId)) {
            throw new ReportException(INCIDENT_NOT_FOUND, "Incident does not exist.");
        }
        var pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt", "id"));
        return reportRepository.findAll((root, query, builder) -> {
            var predicates = new ArrayList<Predicate>();
            predicates.add(builder.isNull(root.get("deletedAt")));
            if (wardId != null) predicates.add(builder.equal(root.get("incident").get("wardId"), wardId));
            if (type != null) predicates.add(builder.equal(root.get("incident").get("type"), type));
            if (incidentId != null) predicates.add(builder.equal(root.get("incident").get("id"), incidentId));
            if (userId != null) predicates.add(builder.equal(root.get("user").get("id"), userId));
            return builder.and(predicates.toArray(Predicate[]::new));
        }, pageable).map(this::toModel);
    }

    @Transactional
    public void deleteReport(long reportId, long userId) {
        if (reportId < 1) throw new ReportException(VALIDATION_ERROR, "Report ID must be positive.");
        ReportEntity report = reportRepository.findVisibleByIdForUpdate(reportId)
                .orElseThrow(() -> new ReportException(REPORT_NOT_FOUND, "Report does not exist."));
        if (report.getUser().getId() != userId) {
            throw new ReportException(REPORT_NOT_OWNED, "Only the report owner can delete it.");
        }
        report.setDeletedAt(OffsetDateTime.now(ZoneOffset.UTC));
        reportRepository.save(report);
    }

    private Report toModel(ReportEntity entity) {
        Report report = new Report();
        report.setId(entity.getId());
        report.setUserId(entity.getUser().getId());
        report.setIncidentId(entity.getIncident().getId());
        report.setWardId(entity.getIncident().getWardId());
        report.setType(entity.getIncident().getType());
        report.setDescription(entity.getDescription());
        report.setLocationDescription(entity.getLocationDescription());
        report.setImageUrls(new ArrayList<>(entity.getImageUrls()));
        report.setLatitude(entity.getLatitude());
        report.setLongitude(entity.getLongitude());
        report.setObservedAt(entity.getObservedAt());
        report.setCreatedAt(entity.getCreatedAt());
        report.setDeletedAt(entity.getDeletedAt());
        return report;
    }
}
