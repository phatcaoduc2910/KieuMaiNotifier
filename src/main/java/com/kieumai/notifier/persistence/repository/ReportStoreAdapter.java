package com.kieumai.notifier.persistence.repository;

import com.kieumai.notifier.business.model.Report;
import com.kieumai.notifier.business.model.ReportPage;
import com.kieumai.notifier.business.model.ReportUpdate;
import com.kieumai.notifier.persistence.entity.IncidentEntity;
import com.kieumai.notifier.persistence.entity.ReportEntity;
import com.kieumai.notifier.persistence.entity.ReportUpdateEntity;
import com.kieumai.notifier.persistence.entity.User;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
@Transactional
public class ReportStoreAdapter implements ReportStore {
    private final ReportRepository reportRepository;
    private final ReportUpdateRepository reportUpdateRepository;
    private final IncidentRepository incidentRepository;
    private final UserRepository userRepository;

    @Override
    public Report save(Report report) {
        User user = userRepository.findById(report.getUserId()).orElseThrow();
        IncidentEntity incident = incidentRepository.findById(report.getIncidentId()).orElseThrow();
        ReportEntity entity = new ReportEntity();
        entity.setUser(user);
        entity.setIncident(incident);
        entity.setDescription(report.getDescription());
        entity.setLatitude(report.getLatitude());
        entity.setLongitude(report.getLongitude());
        entity.setObservedAt(report.getObservedAt());
        entity.setCreatedAt(report.getCreatedAt());
        entity.setDeletedAt(report.getDeletedAt());
        return toModel(reportRepository.saveAndFlush(entity));
    }

    @Override
    @Transactional(readOnly = true)
    public long countActiveByIncidentId(long incidentId) {
        return reportRepository.countByIncidentIdAndDeletedAtIsNull(incidentId);
    }

    @Override
    @Transactional(readOnly = true)
    public long countDistinctActiveUsersByIncidentId(long incidentId) {
        return reportRepository.countDistinctUserIdByIncidentIdAndDeletedAtIsNull(incidentId);
    }

    @Override
    @Transactional(readOnly = true)
    public ReportPage<Report> findReports(Long wardId, String type, Long incidentId, Long userId, int page, int size) {
        var pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt", "id"));
        Page<Report> result = reportRepository.findAll((root, query, builder) -> {
            var predicates = new ArrayList<Predicate>();
            var incident = root.join("incident");
            predicates.add(builder.isNull(root.get("deletedAt")));
            if (wardId != null) predicates.add(builder.equal(incident.get("wardId"), wardId));
            if (type != null) predicates.add(builder.equal(incident.get("type"), type));
            if (incidentId != null) predicates.add(builder.equal(incident.get("id"), incidentId));
            if (userId != null) predicates.add(builder.equal(root.get("user").get("id"), userId));
            return builder.and(predicates.toArray(Predicate[]::new));
        }, pageable).map(this::toModel);

        return new ReportPage<>(
                result.getContent(),
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Report> findVisibleById(long reportId) {
        return reportRepository.findByIdAndDeletedAtIsNull(reportId).map(this::toModel);
    }

    @Override
    public Optional<Report> findVisibleByIdForUpdate(long reportId) {
        return reportRepository.findVisibleByIdForUpdate(reportId).map(this::toModel);
    }

    @Override
    public void softDelete(long reportId, OffsetDateTime deletedAt) {
        reportRepository.findById(reportId).ifPresent(entity -> {
            entity.setDeletedAt(deletedAt);
            reportRepository.saveAndFlush(entity);
        });
    }

    @Override
    public ReportUpdate saveUpdate(ReportUpdate update) {
        User user = userRepository.findById(update.getUserId()).orElseThrow();
        ReportEntity report = reportRepository.findById(update.getReportId()).orElseThrow();
        ReportUpdateEntity entity = new ReportUpdateEntity();
        entity.setUser(user);
        entity.setReport(report);
        entity.setContent(update.getContent());
        entity.setCreatedAt(update.getCreatedAt());
        return toUpdateModel(reportUpdateRepository.saveAndFlush(entity));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReportUpdate> findUpdatesByReportId(long reportId) {
        return reportUpdateRepository.findByReportIdOrderByCreatedAtAscIdAsc(reportId).stream()
                .map(this::toUpdateModel)
                .toList();
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

    private ReportUpdate toUpdateModel(ReportUpdateEntity entity) {
        ReportUpdate update = new ReportUpdate();
        update.setId(entity.getId());
        update.setReportId(entity.getReport().getId());
        update.setUserId(entity.getUser().getId());
        update.setContent(entity.getContent());
        update.setCreatedAt(entity.getCreatedAt());
        return update;
    }
}
