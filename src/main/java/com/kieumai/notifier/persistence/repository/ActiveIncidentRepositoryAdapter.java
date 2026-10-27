package com.kieumai.notifier.persistence.repository;

import com.kieumai.notifier.business.model.ActiveIncident;
import com.kieumai.notifier.persistence.entity.IncidentEntity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public class ActiveIncidentRepositoryAdapter implements ActiveIncidentRepository {

    private static final String ACTIVE = "ACTIVE";

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public List<ActiveIncident> findActiveByWardIds(Collection<Long> wardIds) {
        if (wardIds.isEmpty()) {
            return List.of();
        }
        return entityManager.createQuery(
                        "select i from IncidentEntity i where i.status = :status and i.wardId in :wardIds",
                        IncidentEntity.class)
                .setParameter("status", ACTIVE)
                .setParameter("wardIds", wardIds)
                .getResultList().stream()
                .map(ActiveIncidentRepositoryAdapter::toModel)
                .toList();
    }

    private static ActiveIncident toModel(IncidentEntity i) {
        return new ActiveIncident(
                i.getId(),
                i.getWardId(),
                i.getType(),
                i.getTitle(),
                i.getLatitude() == null ? null : i.getLatitude().doubleValue(),
                i.getLongitude() == null ? null : i.getLongitude().doubleValue(),
                i.getCreatedAt() == null ? null : i.getCreatedAt().toInstant(),
                i.getExpiresAt() == null ? null : i.getExpiresAt().toInstant());
    }
}
