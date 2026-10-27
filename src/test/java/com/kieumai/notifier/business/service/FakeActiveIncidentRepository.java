package com.kieumai.notifier.business.service;

import com.kieumai.notifier.business.model.ActiveIncident;
import com.kieumai.notifier.persistence.repository.ActiveIncidentRepository;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

class FakeActiveIncidentRepository implements ActiveIncidentRepository {

    private final List<ActiveIncident> incidents = new ArrayList<>();

    void addIncident(long incidentId, long wardId, Instant createdAt) {
        incidents.add(new ActiveIncident(incidentId, wardId, "FLOOD", "Incident " + incidentId,
                21.0, 105.0, createdAt, null));
    }

    @Override
    public List<ActiveIncident> findActiveByWardIds(Collection<Long> wardIds) {
        return incidents.stream()
                .filter(incident -> wardIds.contains(incident.wardId()))
                .toList();
    }
}
