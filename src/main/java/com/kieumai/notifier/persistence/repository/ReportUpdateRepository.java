package com.kieumai.notifier.persistence.repository;

import com.kieumai.notifier.persistence.entity.ReportUpdateEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ReportUpdateRepository extends JpaRepository<ReportUpdateEntity, Long> {
    List<ReportUpdateEntity> findByReportIdOrderByCreatedAtAscIdAsc(Long reportId);
}
