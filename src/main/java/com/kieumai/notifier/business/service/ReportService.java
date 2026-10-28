package com.kieumai.notifier.business.service;

import com.kieumai.notifier.business.exception.ReportException;
import com.kieumai.notifier.business.model.Incident;
import com.kieumai.notifier.business.model.Report;
import com.kieumai.notifier.business.model.ReportPage;
import com.kieumai.notifier.business.model.ReportUpdate;
import com.kieumai.notifier.config.ReportGroupingProperties;
import com.kieumai.notifier.persistence.repository.IncidentStore;
import com.kieumai.notifier.persistence.repository.ReportStore;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import static com.kieumai.notifier.business.exception.ReportException.Code.*;

@Service
@RequiredArgsConstructor
@Transactional
public class ReportService {
    private static final int REPORTS_TO_ACTIVATE = 3;
    private static final Set<String> TYPES = Set.of("FLOOD", "TRAFFIC_JAM", "OTHER");
    private final ReportStore reportStore;
    private final IncidentStore incidentStore;
    private final ReportGroupingProperties properties;

    public Report createReport(Report report) {
        // Lock an existing ward row so concurrent first reports cannot create duplicate incidents.
        if (!incidentStore.lockWard(report.getWardId())) {
            throw new ReportException(WARD_NOT_FOUND, "Ward does not exist.");
        }
        if (incidentStore.findUserById(report.getUserId()).isEmpty()) {
            throw new ReportException(UNAUTHORIZED, "User does not exist.");
        }
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        Incident incident = findIncident(report, now);
        if (incident == null) {
            incident = new Incident();
            incident.setWardId(report.getWardId());
            incident.setType(report.getType());
            incident.setLatitude(report.getLatitude());
            incident.setLongitude(report.getLongitude());
            String title = report.getDescription().strip();
            incident.setTitle(title.substring(0, Math.min(title.length(), 255)));
            incident.setStatus("PENDING");
            incident.setStartedAt(report.getObservedAt().withOffsetSameInstant(ZoneOffset.UTC));
            incident.setCreatedAt(now);
            incident.setExpiresAt(now.plusSeconds(properties.getIncidentLifetimeSeconds()));
            incident = incidentStore.save(incident);
        } else {
            incident.setExpiresAt(now.plusSeconds(properties.getIncidentLifetimeSeconds()));
            incident = incidentStore.save(incident);
        }

        Report toSave = new Report();
        toSave.setUserId(report.getUserId());
        toSave.setIncidentId(incident.getId());
        toSave.setWardId(incident.getWardId());
        toSave.setType(incident.getType());
        toSave.setDescription(report.getDescription().strip());
        toSave.setLatitude(report.getLatitude());
        toSave.setLongitude(report.getLongitude());
        toSave.setObservedAt(report.getObservedAt().withOffsetSameInstant(ZoneOffset.UTC));
        toSave.setCreatedAt(now);
        Report saved = reportStore.save(toSave);

        if ("PENDING".equals(incident.getStatus())
                && reportStore.countDistinctActiveUsersByIncidentId(incident.getId()) >= REPORTS_TO_ACTIVATE) {
            incident.setStatus("ACTIVE");
            incidentStore.save(incident);
        }
        return saved;
    }

    private Incident findIncident(Report report, OffsetDateTime now) {
        return incidentStore.findMergeCandidates(
                        report.getWardId(), report.getType(), List.of("PENDING", "ACTIVE", "ALERTED"), now).stream()
                .map(incident -> new IncidentDistance(incident, GeoUtils.distanceMeters(
                        report.getLatitude().doubleValue(), report.getLongitude().doubleValue(),
                        incident.getLatitude().doubleValue(), incident.getLongitude().doubleValue())))
                .filter(candidate -> candidate.distance() <= properties.getGroupingRadiusMeters())
                .min(Comparator.comparingDouble(IncidentDistance::distance)
                        .thenComparing(candidate -> candidate.incident().getId()))
                .map(IncidentDistance::incident).orElse(null);
    }

    private record IncidentDistance(Incident incident, double distance) {}

    @Transactional(readOnly = true)
    public ReportPage<Report> getReports(Long wardId, String type, Long incidentId, Long userId, int page, int size) {
        if (page < 0 || size < 1 || size > 100 || (wardId != null && wardId < 1)
                || (incidentId != null && incidentId < 1) || (type != null && !TYPES.contains(type))) {
            throw new ReportException(VALIDATION_ERROR, "Invalid report filter or pagination.");
        }
        if (incidentId != null && !incidentStore.existsById(incidentId)) {
            throw new ReportException(INCIDENT_NOT_FOUND, "Incident does not exist.");
        }
        return reportStore.findReports(wardId, type, incidentId, userId, page, size);
    }

    public void deleteReport(long reportId, long userId) {
        if (reportId < 1) throw new ReportException(VALIDATION_ERROR, "Report ID must be positive.");
        Report existing = visibleReport(reportId);
        if (!incidentStore.lockWard(existing.getWardId())) {
            throw new ReportException(WARD_NOT_FOUND, "Ward does not exist.");
        }
        Report report = reportStore.findVisibleByIdForUpdate(reportId)
                .orElseThrow(() -> new ReportException(REPORT_NOT_FOUND, "Report does not exist."));
        requireOwner(report, userId);
        reportStore.softDelete(reportId, OffsetDateTime.now(ZoneOffset.UTC));
    }

    public ReportUpdate createReportUpdate(long reportId, long userId, String content) {
        if (reportId < 1) throw new ReportException(VALIDATION_ERROR, "Report ID must be positive.");
        if (content == null || content.isBlank() || content.strip().length() > 2000) {
            throw new ReportException(VALIDATION_ERROR, "Invalid report update content.");
        }
        Report report = visibleReport(reportId);
        if (incidentStore.findUserById(userId).isEmpty()) {
            throw new ReportException(UNAUTHORIZED, "User does not exist.");
        }
        ReportUpdate update = new ReportUpdate();
        update.setReportId(report.getId());
        update.setUserId(userId);
        update.setContent(content.strip());
        update.setCreatedAt(OffsetDateTime.now(ZoneOffset.UTC));
        return reportStore.saveUpdate(update);
    }

    @Transactional(readOnly = true)
    public List<ReportUpdate> getReportUpdates(long reportId) {
        if (reportId < 1) throw new ReportException(VALIDATION_ERROR, "Report ID must be positive.");
        visibleReport(reportId);
        return reportStore.findUpdatesByReportId(reportId);
    }

    private Report visibleReport(long reportId) {
        return reportStore.findVisibleById(reportId)
                .orElseThrow(() -> new ReportException(REPORT_NOT_FOUND, "Report does not exist."));
    }

    private void requireOwner(Report report, long userId) {
        if (!Objects.equals(report.getUserId(), userId)) {
            throw new ReportException(REPORT_NOT_OWNED, "Only the report owner can change it.");
        }
    }
}
