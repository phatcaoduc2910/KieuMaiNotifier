package com.kieumai.notifier.persistence.repository;

import com.kieumai.notifier.business.model.Incident;
import com.kieumai.notifier.business.model.User;

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface IncidentStore {
    boolean lockWard(long wardId);

    Optional<User> findUserById(long userId);

    Optional<Incident> findById(long incidentId);

    boolean existsById(long incidentId);

    List<Incident> findByFilter(Long wardId, String type);

    List<Incident> findByStatus(String status);

    List<Incident> findMergeCandidates(long wardId, String type, Collection<String> statuses, OffsetDateTime now);

    Incident save(Incident incident);

    void expireIncidentsBefore(Collection<String> statuses, OffsetDateTime now);
}
