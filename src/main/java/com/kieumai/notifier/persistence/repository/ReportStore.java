package com.kieumai.notifier.persistence.repository;

import com.kieumai.notifier.business.model.Report;
import com.kieumai.notifier.business.model.ReportPage;
import com.kieumai.notifier.business.model.ReportUpdate;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

public interface ReportStore {
    Report save(Report report);

    long countActiveByIncidentId(long incidentId);

    long countDistinctActiveUsersByIncidentId(long incidentId);

    ReportPage<Report> findReports(Long wardId, String type, Long incidentId, Long userId, int page, int size);

    Optional<Report> findVisibleById(long reportId);

    Optional<Report> findVisibleByIdForUpdate(long reportId);

    void softDelete(long reportId, OffsetDateTime deletedAt);

    ReportUpdate saveUpdate(ReportUpdate update);

    List<ReportUpdate> findUpdatesByReportId(long reportId);
}
