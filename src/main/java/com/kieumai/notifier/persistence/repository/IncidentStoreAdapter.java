package com.kieumai.notifier.persistence.repository;

import com.kieumai.notifier.business.model.Incident;
import com.kieumai.notifier.business.model.User;
import com.kieumai.notifier.persistence.entity.IncidentEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
@Transactional
public class IncidentStoreAdapter implements IncidentStore {
    private final IncidentRepository incidentRepository;
    private final ReportRepository reportRepository;
    private final WardRepository wardRepository;
    private final UserRepository userRepository;

    @Override
    public boolean lockWard(long wardId) {
        return wardRepository.findByIdForUpdate(wardId).isPresent();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<User> findUserById(long userId) {
        return userRepository.findById(userId).map(this::toUserModel);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Incident> findById(long incidentId) {
        return incidentRepository.findById(incidentId).map(this::toModelWithCount);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsById(long incidentId) {
        return incidentRepository.existsById(incidentId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Incident> findByFilter(Long wardId, String type) {
        List<IncidentEntity> incidents;
        if (wardId != null && type != null) {
            incidents = incidentRepository.findByWardIdAndType(wardId, type);
        } else if (wardId != null) {
            incidents = incidentRepository.findByWardId(wardId);
        } else if (type != null) {
            incidents = incidentRepository.findByType(type);
        } else {
            incidents = incidentRepository.findAll();
        }
        return incidents.stream().map(this::toModelWithCount).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Incident> findByStatus(String status) {
        return incidentRepository.findByStatus(status).stream()
                .map(this::toModelWithCount)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Incident> findMergeCandidates(long wardId, String type, Collection<String> statuses, OffsetDateTime now) {
        return incidentRepository.findByWardIdAndTypeAndStatusInAndExpiresAtAfterOrderByIdAsc(
                        wardId, type, statuses, now).stream()
                .map(this::toModel)
                .toList();
    }

    @Override
    public Incident save(Incident incident) {
        IncidentEntity entity = incident.getId() == null
                ? new IncidentEntity()
                : incidentRepository.findById(incident.getId()).orElseGet(IncidentEntity::new);
        entity.setWardId(incident.getWardId());
        entity.setType(incident.getType());
        entity.setTitle(incident.getTitle());
        entity.setLatitude(incident.getLatitude());
        entity.setLongitude(incident.getLongitude());
        entity.setStatus(incident.getStatus());
        entity.setStartedAt(incident.getStartedAt());
        entity.setExpiresAt(incident.getExpiresAt());
        entity.setAlertedAt(incident.getAlertedAt());
        entity.setCreatedAt(incident.getCreatedAt());
        return toModel(incidentRepository.saveAndFlush(entity));
    }

    @Override
    public void expireIncidentsBefore(Collection<String> statuses, OffsetDateTime now) {
        incidentRepository.findByStatusInAndExpiresAtBefore(statuses, now)
                .forEach(incident -> incident.setStatus("EXPIRED"));
    }

    private Incident toModel(IncidentEntity entity) {
        Incident model = new Incident();
        model.setId(entity.getId());
        model.setWardId(entity.getWardId());
        model.setType(entity.getType());
        model.setTitle(entity.getTitle());
        model.setLatitude(entity.getLatitude());
        model.setLongitude(entity.getLongitude());
        model.setStatus(entity.getStatus());
        model.setStartedAt(entity.getStartedAt());
        model.setExpiresAt(entity.getExpiresAt());
        model.setAlertedAt(entity.getAlertedAt());
        model.setCreatedAt(entity.getCreatedAt());
        return model;
    }

    private Incident toModelWithCount(IncidentEntity entity) {
        Incident model = toModel(entity);
        model.setReportCount(reportRepository.countByIncidentIdAndDeletedAtIsNull(entity.getId()));
        return model;
    }

    private User toUserModel(com.kieumai.notifier.persistence.entity.User entity) {
        User model = new User();
        model.setId(entity.getId());
        model.setUsername(entity.getUsername());
        model.setDisplayName(entity.getDisplayName());
        model.setLatitude(entity.getLatitude());
        model.setLongitude(entity.getLongitude());
        model.setCreatedAt(entity.getCreatedAt());
        return model;
    }
}
