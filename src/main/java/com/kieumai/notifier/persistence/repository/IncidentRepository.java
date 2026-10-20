package com.kieumai.notifier.persistence.repository;

import com.kieumai.notifier.persistence.entity.IncidentEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface IncidentRepository extends JpaRepository<IncidentEntity, Long> {
    List<IncidentEntity> findByWardIdAndStatus(Long wardId, String status);
}
