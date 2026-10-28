package com.kieumai.notifier.persistence.repository;

import com.kieumai.notifier.persistence.entity.ReportEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.Optional;

public interface ReportRepository extends JpaRepository<ReportEntity, Long>, JpaSpecificationExecutor<ReportEntity> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from ReportEntity r where r.id = :id and r.deletedAt is null")
    Optional<ReportEntity> findVisibleByIdForUpdate(@Param("id") Long id);

    Optional<ReportEntity> findByIdAndDeletedAtIsNull(Long id);
    long countByIncidentIdAndDeletedAtIsNull(Long incidentId);

    @Query("select count(distinct r.user.id) from ReportEntity r where r.incident.id = :incidentId and r.deletedAt is null")
    long countDistinctUserIdByIncidentIdAndDeletedAtIsNull(@Param("incidentId") Long incidentId);
}
