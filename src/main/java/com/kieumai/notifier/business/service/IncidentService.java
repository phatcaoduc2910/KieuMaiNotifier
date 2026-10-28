package com.kieumai.notifier.business.service;

import com.kieumai.notifier.business.exception.ReportException;
import com.kieumai.notifier.business.model.Incident;
import com.kieumai.notifier.business.model.User;
import com.kieumai.notifier.persistence.repository.IncidentStore;
import com.kieumai.notifier.persistence.repository.SubscriptionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.stream.Collectors;

import static com.kieumai.notifier.business.exception.ReportException.Code.INCIDENT_NOT_FOUND;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class IncidentService {
    private final IncidentStore incidentStore;
    private final SubscriptionRepository subscriptionRepository;

    private static final double MAX_DISTANCE = 1000.0;

    public List<Incident> getIncidents(Long wardId, String type) {
        return incidentStore.findByFilter(wardId, type);
    }

    public Incident getIncidentDetail(Long incidentId) {
        return findIncident(incidentId);
    }

    @Transactional
    public void resolveIncidentEarly(Long incidentId) {
        var existing = findIncident(incidentId);
        incidentStore.lockWard(existing.getWardId());
        // Reload after acquiring the same ward lock used by report creation.
        var incident = findIncident(incidentId);
        incident.setStatus("RESOLVED");
        incident.setExpiresAt(OffsetDateTime.now(ZoneOffset.UTC));
        incidentStore.save(incident);
    }

    private Incident findIncident(Long incidentId) {
        return incidentStore.findById(incidentId)
                .orElseThrow(() -> new ReportException(INCIDENT_NOT_FOUND, "Incident does not exist."));
    }

    // Trả về list các incident thoả mãn(cùng phường/ bán kính 1km)
    @Transactional
    public List<Incident> getFeed(Long userId) {
        User user = incidentStore.findUserById(userId)
                .orElseThrow(() -> new RuntimeException("Người dùng không tồn tại với ID: " + userId));

        List<Long> subscribedWardIds = subscriptionRepository.findWardIdsByUserId(userId);
        List<Incident> activeIncidents = incidentStore.findByStatus("ACTIVE");

        return activeIncidents.stream()
                .filter(incident -> isEligibleForFeed(user, subscribedWardIds, incident))
                .collect(Collectors.toList());
    }

    private boolean isEligibleForFeed(User user, List<Long> subscribedWardIds, Incident incident) {
        boolean isSubscribedWard = incident.getWardId() != null
                && subscribedWardIds.contains(incident.getWardId());

        if (isSubscribedWard) return true;

        if (user.getLatitude() != null && user.getLongitude() != null
                && incident.getLatitude() != null && incident.getLongitude() != null) {
            double distance = GeoUtils.distanceMeters(
                    user.getLatitude().doubleValue(),
                    user.getLongitude().doubleValue(),
                    incident.getLatitude().doubleValue(),
                    incident.getLongitude().doubleValue());
            return distance <= MAX_DISTANCE;
        }

        return false;
    }
}
