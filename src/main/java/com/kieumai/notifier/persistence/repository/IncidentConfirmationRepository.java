package com.kieumai.notifier.persistence.repository;

import com.kieumai.notifier.persistence.entity.IncidentConfirmationEntity;
import com.kieumai.notifier.persistence.entity.IncidentConfirmationId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IncidentConfirmationRepository
        extends JpaRepository<IncidentConfirmationEntity, IncidentConfirmationId> {
    long countByIdIncidentId(Long incidentId);

    void deleteByIdIncidentIdAndUserId(Long incidentId, Long userId);
}
