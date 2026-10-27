package com.kieumai.notifier.business.service;

import com.kieumai.notifier.business.model.IncidentAlert;
import com.kieumai.notifier.business.model.NearbyIncident;
import com.kieumai.notifier.persistence.repository.IncidentAlertRepository;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

class FakeIncidentAlertRepository implements IncidentAlertRepository {

    private final List<IncidentAlert> activeIncidents = new ArrayList<>();
    private final Set<Long> alertedIncidentIds = new HashSet<>();
    private final List<NearbyIncident> nearbyIncidents = new ArrayList<>();

    void addActiveIncident(long incidentId, long wardId, String type, String title) {
        activeIncidents.add(new IncidentAlert(incidentId, wardId, type, title));
    }

    void addNearbyIncident(long incidentId, double latitude, double longitude) {
        nearbyIncidents.add(new NearbyIncident(incidentId, 1L, "FLOOD", "Incident " + incidentId,
                latitude, longitude, null));
    }

    boolean isAlerted(long incidentId) {
        return alertedIncidentIds.contains(incidentId);
    }

    @Override
    public List<IncidentAlert> findActiveNotAlerted() {
        return activeIncidents.stream()
                .filter(alert -> !alertedIncidentIds.contains(alert.incidentId()))
                .toList();
    }

    @Override
    public boolean markAlerted(long incidentId, Instant alertedAt) {
        return alertedIncidentIds.add(incidentId);
    }

    @Override
    public List<NearbyIncident> findActiveInBox(double minLatitude, double maxLatitude,
                                                double minLongitude, double maxLongitude) {
        return nearbyIncidents.stream()
                .filter(i -> i.latitude() >= minLatitude && i.latitude() <= maxLatitude)
                .filter(i -> i.longitude() >= minLongitude && i.longitude() <= maxLongitude)
                .toList();
    }
}
