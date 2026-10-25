package com.kieumai.notifier.persistence.repository;

import com.kieumai.notifier.persistence.entity.ReportGroupEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;

public interface ReportGroupRepository extends JpaRepository<ReportGroupEntity, Long> {
    List<ReportGroupEntity> findByWardIdAndTypeOrderByIdAsc(Long wardId, String type);

    @Modifying(flushAutomatically = true)
    @Query("""
            update IncidentEntity i set i.expiresAt = :deadline
            where i.id = :id and i.status in ('PENDING', 'ACTIVE', 'ALERTED') and i.expiresAt > :now
            """)
    int renewOpenIncident(@Param("id") Long incidentId, @Param("now") OffsetDateTime now,
                          @Param("deadline") OffsetDateTime deadline);
}
