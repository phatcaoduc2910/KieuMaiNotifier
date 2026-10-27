package com.kieumai.notifier.business.service;

import com.kieumai.notifier.business.exception.ReportException;
import com.kieumai.notifier.business.model.Report;
import com.kieumai.notifier.config.ReportGroupingProperties;
import com.kieumai.notifier.persistence.entity.*;
import com.kieumai.notifier.persistence.repository.*;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.*;

import static com.kieumai.notifier.business.exception.ReportException.Code.*;

@Service
@RequiredArgsConstructor
@Transactional
public class ReportService {
    private static final int REPORTS_TO_ACTIVATE = 3;
    private static final Set<String> TYPES = Set.of("FLOOD", "TRAFFIC_JAM", "OTHER");
    private final ReportRepository reportRepository;
    private final IncidentRepository incidentRepository;
    private final UserRepository userRepository;
    private final WardRepository wardRepository;
    private final ReportGroupingProperties properties;

    public Report createReport(Report report) {
        // Lock an existing ward row so concurrent first reports cannot create duplicate incidents.
        wardRepository.findByIdForUpdate(report.getWardId())
                .orElseThrow(() -> new ReportException(WARD_NOT_FOUND, "Ward does not exist."));
        User user = userRepository.findById(report.getUserId())
                .orElseThrow(() -> new ReportException(UNAUTHORIZED, "User does not exist."));
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        IncidentEntity incident = findIncident(report, now);
        if (incident == null) {
            incident = new IncidentEntity();
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
            incident = incidentRepository.saveAndFlush(incident);
        } else {
            incident.setExpiresAt(now.plusSeconds(properties.getIncidentLifetimeSeconds()));
        }

        ReportEntity entity = new ReportEntity();
        entity.setUser(user);
        entity.setIncident(incident);
        entity.setDescription(report.getDescription().strip());
        entity.setLatitude(report.getLatitude());
        entity.setLongitude(report.getLongitude());
        entity.setObservedAt(report.getObservedAt().withOffsetSameInstant(ZoneOffset.UTC));
        entity.setCreatedAt(now);
        entity = reportRepository.saveAndFlush(entity);

        if ("PENDING".equals(incident.getStatus())
                && reportRepository.countByIncidentIdAndDeletedAtIsNull(incident.getId()) >= REPORTS_TO_ACTIVATE) {
            incident.setStatus("ACTIVE");
        }
        return toModel(entity);
    }

    private IncidentEntity findIncident(Report report, OffsetDateTime now) {
        return incidentRepository.findByWardIdAndTypeAndStatusInAndExpiresAtAfterOrderByIdAsc(
                        report.getWardId(), report.getType(), List.of("PENDING", "ACTIVE", "ALERTED"), now).stream()
                .map(incident -> new IncidentDistance(incident, GeoUtils.distanceMeters(
                        report.getLatitude().doubleValue(), report.getLongitude().doubleValue(),
                        incident.getLatitude().doubleValue(), incident.getLongitude().doubleValue())))
                .filter(candidate -> candidate.distance() <= properties.getGroupingRadiusMeters())
                .min(Comparator.comparingDouble(IncidentDistance::distance)
                        .thenComparing(candidate -> candidate.incident().getId()))
                .map(IncidentDistance::incident).orElse(null);
    }

    private record IncidentDistance(IncidentEntity incident, double distance) {}

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
            var incident = root.join("incident");
            predicates.add(builder.isNull(root.get("deletedAt")));
            if (wardId != null) predicates.add(builder.equal(incident.get("wardId"), wardId));
            if (type != null) predicates.add(builder.equal(incident.get("type"), type));
            if (incidentId != null) predicates.add(builder.equal(incident.get("id"), incidentId));
            if (userId != null) predicates.add(builder.equal(root.get("user").get("id"), userId));
            return builder.and(predicates.toArray(Predicate[]::new));
        }, pageable).map(this::toModel);
    }

    public void deleteReport(long reportId, long userId) {
        if (reportId < 1) throw new ReportException(VALIDATION_ERROR, "Report ID must be positive.");
        ReportEntity existing = visibleReport(reportId);
        wardRepository.findByIdForUpdate(existing.getIncident().getWardId()).orElseThrow();
        ReportEntity report = reportRepository.findVisibleByIdForUpdate(reportId)
                .orElseThrow(() -> new ReportException(REPORT_NOT_FOUND, "Report does not exist."));
        requireOwner(report, userId);
        report.setDeletedAt(OffsetDateTime.now(ZoneOffset.UTC));
    }

    private ReportEntity visibleReport(long reportId) {
        return reportRepository.findByIdAndDeletedAtIsNull(reportId)
                .orElseThrow(() -> new ReportException(REPORT_NOT_FOUND, "Report does not exist."));
    }

    private void requireOwner(ReportEntity report, long userId) {
        if (!Objects.equals(report.getUser().getId(), userId)) {
            throw new ReportException(REPORT_NOT_OWNED, "Only the report owner can change it.");
        }
    }

    private Report toModel(ReportEntity entity) {
        Report report = new Report();
        report.setId(entity.getId());
        report.setUserId(entity.getUser().getId());
        report.setIncidentId(entity.getIncident().getId());
        report.setWardId(entity.getIncident().getWardId());
        report.setType(entity.getIncident().getType());
        report.setDescription(entity.getDescription());
        report.setLatitude(entity.getLatitude());
        report.setLongitude(entity.getLongitude());
        report.setObservedAt(entity.getObservedAt());
        report.setCreatedAt(entity.getCreatedAt());
        report.setDeletedAt(entity.getDeletedAt());
        return report;
    }
}
