package com.kieumai.notifier.persistence.repository;

import com.kieumai.notifier.persistence.entity.IncidentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;

public interface IncidentRepository extends JpaRepository<IncidentEntity, Long> {
    List<IncidentEntity> findByWardIdAndType(Long wardId, String type);
    List<IncidentEntity> findByWardId(Long wardId);
    List<IncidentEntity> findByType(String type);
    List<IncidentEntity> findByStatusInAndExpiresAtBefore(Collection<String> statuses, OffsetDateTime now);
    List<IncidentEntity> findByWardIdAndTypeAndStatusInAndExpiresAtAfterOrderByIdAsc(
            Long wardId, String type, Collection<String> statuses, OffsetDateTime now);

    List<IncidentEntity> findByStatus(String status);
}
