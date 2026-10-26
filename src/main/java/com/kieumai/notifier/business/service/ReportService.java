package com.kieumai.notifier.business.service;

import com.kieumai.notifier.business.exception.ReportException;
import com.kieumai.notifier.business.model.Report;
import com.kieumai.notifier.config.ReportGroupingProperties;
import com.kieumai.notifier.persistence.entity.IncidentEntity;
import com.kieumai.notifier.persistence.entity.ReportEntity;
import com.kieumai.notifier.persistence.entity.ReportGroupEntity;
import com.kieumai.notifier.persistence.entity.ReportGroupingLock;
import com.kieumai.notifier.persistence.repository.IncidentRepository;
import com.kieumai.notifier.persistence.repository.ReportRepository;
import com.kieumai.notifier.persistence.repository.ReportGroupRepository;
import com.kieumai.notifier.persistence.repository.ReportGroupingLockRepository;
import com.kieumai.notifier.persistence.repository.UserRepository;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Objects;
import java.util.Set;

import static com.kieumai.notifier.business.exception.ReportException.Code.*;

@Service
@RequiredArgsConstructor
public class ReportService {
    private static final int REPORTS_TO_CREATE_INCIDENT = 3;
    private static final Set<String> TYPES = Set.of("FLOOD", "TRAFFIC_JAM", "OTHER");

    private final ReportRepository reportRepository;
    private final IncidentRepository incidentRepository;
    private final UserRepository userRepository;
    private final ReportGroupRepository groupRepository;
    private final ReportGroupingLockRepository groupingLocks;
    private final ReportGroupingProperties properties;
    private final PlatformTransactionManager transactionManager;
    private final EntityManager entityManager;

    public Report createReport(Report report) {
        String key = groupingKey(report.getWardId(), report.getType());
        ensureGroupingLock(key);
        // Keep the mutex until the report and all incident links have committed.
        return Objects.requireNonNull(new TransactionTemplate(transactionManager)
                .execute(status -> createReportUnderLock(report, key)));
    }

    private Report createReportUnderLock(Report report, String key) {
        groupingLocks.findByKeyForUpdate(key).orElseThrow();
        var user = userRepository.findById(report.getUserId())
                .orElseThrow(() -> new ReportException(UNAUTHORIZED, "User does not exist."));
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        ReportGroupEntity group = findGroup(report, now);
        if (group == null) {
            group = new ReportGroupEntity();
            group.setWardId(report.getWardId());
            group.setType(report.getType());
            group.setLatitude(report.getLatitude());
            group.setLongitude(report.getLongitude());
            group.setCreatedAt(now);
            group = groupRepository.saveAndFlush(group);
        }

        ReportEntity entity = new ReportEntity();
        entity.setUser(user);
        entity.setWardId(report.getWardId());
        entity.setType(report.getType());
        entity.setGroup(group);
        entity.setIncident(group.getIncident());
        entity.setDescription(report.getDescription().strip());
        entity.setLocationDescription(report.getLocationDescription());
        entity.setImageUrls(new ArrayList<>(report.getImageUrls()));
        entity.setLatitude(report.getLatitude());
        entity.setLongitude(report.getLongitude());
        entity.setObservedAt(report.getObservedAt().withOffsetSameInstant(ZoneOffset.UTC));
        entity.setCreatedAt(now);
        entity = reportRepository.saveAndFlush(entity);

        if (group.getIncident() == null
                && reportRepository.countByGroupIdAndDeletedAtIsNull(group.getId()) >= REPORTS_TO_CREATE_INCIDENT) {
            var members = reportRepository.findByGroupIdAndDeletedAtIsNullOrderByCreatedAtAscIdAsc(group.getId());
            var first = members.getFirst();
            IncidentEntity incident = new IncidentEntity();
            incident.setWardId(group.getWardId());
            incident.setType(group.getType());
            incident.setLatitude(group.getLatitude());
            incident.setLongitude(group.getLongitude());
            String title = first.getDescription();
            incident.setTitle(title.substring(0, Math.min(title.length(), 255)));
            incident.setStatus("ACTIVE");
            incident.setStartedAt(first.getObservedAt());
            incident.setCreatedAt(now);
            incident.setExpiresAt(now.plusSeconds(properties.getIncidentLifetimeSeconds()));
            incidentRepository.saveAndFlush(incident);
            group.setIncident(incident);
            for (ReportEntity member : members) member.setIncident(incident);
            reportRepository.saveAll(members);
            groupRepository.save(group);
        }
        return toModel(entity);
    }

    private void ensureGroupingLock(String key) {
        if (groupingLocks.existsById(key)) return;
        try {
            // Commit mutex creation separately so concurrent first requests can use the same row.
            new TransactionTemplate(transactionManager).execute(status ->
                    groupingLocks.saveAndFlush(new ReportGroupingLock(key)));
        } catch (DataIntegrityViolationException exception) {
            if (!groupingLocks.existsById(key)) throw exception;
        }
    }

    private ReportGroupEntity findGroup(Report report, OffsetDateTime now) {
        var candidates = groupRepository.findByWardIdAndTypeOrderByIdAsc(report.getWardId(), report.getType()).stream()
                .map(group -> new GroupDistance(group, distanceMeters(report.getLatitude().doubleValue(),
                        report.getLongitude().doubleValue(), group.getLatitude().doubleValue(), group.getLongitude().doubleValue())))
                .filter(candidate -> candidate.distance() <= properties.getGroupingRadiusMeters())
                .sorted(Comparator.comparingDouble(GroupDistance::distance)
                        .thenComparing(candidate -> candidate.group().getId()))
                .toList();
        for (GroupDistance candidate : candidates) {
            ReportGroupEntity group = candidate.group();
            if (group.getIncident() == null) return group;
            if (groupRepository.renewOpenIncident(group.getIncident().getId(), now,
                    now.plusSeconds(properties.getIncidentLifetimeSeconds())) == 1) return group;
        }
        return null;
    }

    private record GroupDistance(ReportGroupEntity group, double distance) {}

    private static String groupingKey(Long wardId, String type) { return wardId + ":" + type; }

    private static double distanceMeters(double latitude1, double longitude1, double latitude2, double longitude2) {
        double latitudeDelta = Math.toRadians(latitude2 - latitude1);
        double longitudeDelta = Math.toRadians(longitude2 - longitude1);
        double a = Math.pow(Math.sin(latitudeDelta / 2), 2)
                + Math.cos(Math.toRadians(latitude1)) * Math.cos(Math.toRadians(latitude2))
                * Math.pow(Math.sin(longitudeDelta / 2), 2);
        return 6_371_000 * 2 * Math.asin(Math.sqrt(Math.max(0, Math.min(1, a))));
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
            var incident = root.join("incident", JoinType.LEFT);
            predicates.add(builder.isNull(root.get("deletedAt")));
            if (wardId != null) predicates.add(builder.equal(builder.coalesce(root.get("wardId"), incident.get("wardId")), wardId));
            if (type != null) predicates.add(builder.equal(builder.coalesce(root.get("type"), incident.get("type")), type));
            if (incidentId != null) predicates.add(builder.equal(incident.get("id"), incidentId));
            if (userId != null) predicates.add(builder.equal(root.get("user").get("id"), userId));
            return builder.and(predicates.toArray(Predicate[]::new));
        }, pageable).map(this::toModel);
    }

    @Transactional
    public void deleteReport(long reportId, long userId) {
        if (reportId < 1) throw new ReportException(VALIDATION_ERROR, "Report ID must be positive.");
        ReportEntity existing = reportRepository.findById(reportId)
                .orElseThrow(() -> new ReportException(REPORT_NOT_FOUND, "Report does not exist."));
        if (existing.getGroup() != null) {
            // Use the same lock order as creation so a deleted pending report cannot count toward three.
            groupingLocks.findByKeyForUpdate(groupingKey(existing.getWardId(), existing.getType())).orElseThrow();
        }
        ReportEntity report = reportRepository.findVisibleByIdForUpdate(reportId)
                .orElseThrow(() -> new ReportException(REPORT_NOT_FOUND, "Report does not exist."));
        entityManager.refresh(report);
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
        var incident = entity.getIncident();
        report.setIncidentId(incident == null ? null : incident.getId());
        report.setWardId(entity.getWardId() != null ? entity.getWardId() : incident.getWardId());
        report.setType(entity.getType() != null ? entity.getType() : incident.getType());
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
