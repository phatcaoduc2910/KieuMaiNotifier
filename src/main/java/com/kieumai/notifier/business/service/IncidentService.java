package com.kieumai.notifier.business.service;

import com.kieumai.notifier.business.exception.ReportException;
import com.kieumai.notifier.persistence.entity.IncidentEntity;
import com.kieumai.notifier.persistence.entity.User;
import com.kieumai.notifier.persistence.repository.*;
import com.kieumai.notifier.presentation.dto.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.stream.Collectors;

import static com.kieumai.notifier.business.exception.ReportException.Code.*;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class IncidentService {
    private final IncidentRepository incidentRepository;
    private final ReportRepository reportRepository;
    private final WardRepository wardRepository;
    private final UserRepository userRepository;
    private final SubscriptionRepository subscriptionRepository;

    private static final double MAX_DISTANCE_KM = 1.0;

    public List<IncidentResponseDto> getIncidents(Long wardId, String type) {
        List<IncidentEntity> incidents;
        if (wardId != null && type != null) incidents = incidentRepository.findByWardIdAndType(wardId, type);
        else if (wardId != null) incidents = incidentRepository.findByWardId(wardId);
        else if (type != null) incidents = incidentRepository.findByType(type);
        else incidents = incidentRepository.findAll();
        return incidents.stream().map(this::toResponse).toList();
    }

    public IncidentDetailDto getIncidentDetail(Long incidentId) {
        var incident = findIncident(incidentId);
        var response = new IncidentDetailDto();
        response.setId(incident.getId());
        response.setWardId(incident.getWardId());
        response.setType(incident.getType());
        response.setStatus(incident.getStatus());
        response.setLatitude(incident.getLatitude());
        response.setLongitude(incident.getLongitude());
        response.setTitle(incident.getTitle());
        response.setReportCount(reportRepository.countByIncidentIdAndDeletedAtIsNull(incidentId));
        response.setAlertedAt(incident.getAlertedAt());
        response.setExpiresAt(incident.getExpiresAt());
        response.setCreatedAt(incident.getCreatedAt());
        return response;
    }

    @Transactional
    public void resolveIncidentEarly(Long incidentId) {
        var existing = findIncident(incidentId);
        wardRepository.findByIdForUpdate(existing.getWardId()).orElseThrow();
        // Reload after acquiring the same ward lock used by report creation.
        var incident = incidentRepository.findById(incidentId).orElseThrow();
        incident.setStatus("RESOLVED");
        incident.setExpiresAt(OffsetDateTime.now(ZoneOffset.UTC));
    }

    private IncidentEntity findIncident(Long incidentId) {
        return incidentRepository.findById(incidentId)
                .orElseThrow(() -> new ReportException(INCIDENT_NOT_FOUND, "Incident does not exist."));
    }

    // Trả về list các incident thoả mãn(cùng phường/ bán kính 1km)
    @Transactional
    public List<IncidentResponseDto> getFeed(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Người dùng không tồn tại với ID: " + userId));

        List<Long> subscribedWardIds = subscriptionRepository.findWardIdsByUserId(userId);
        List<IncidentEntity> activeIncidents = incidentRepository.findByStatus("ACTIVE");

        return activeIncidents.stream()
                .filter(incident -> isEligibleForFeed(user, subscribedWardIds, incident))
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    private boolean isEligibleForFeed(User user, List<Long> subscribedWardIds, IncidentEntity incident) {
        boolean isSubscribedWard = incident.getWard() != null
                && subscribedWardIds.contains(incident.getWardId());

        if(isSubscribedWard) return true;

        if(user.getLatitude() != null && user.getLongitude() != null
        && incident.getLatitude() != null && incident.getLongitude() != null) {
            double distance = GeoUtils.distanceMeters(
                    user.getLatitude().doubleValue(),
                    user.getLongitude().doubleValue(),
                    incident.getLatitude().doubleValue(),
                    incident.getLongitude().doubleValue());
            return distance <= MAX_DISTANCE_KM;
        }

        return false;
    }

    private IncidentResponseDto toResponse(IncidentEntity incident) {
        var response = new IncidentResponseDto();
        response.setId(incident.getId());
        response.setWardId(incident.getWardId());
        response.setType(incident.getType());
        response.setStatus(incident.getStatus());
        response.setLatitude(incident.getLatitude());
        response.setLongitude(incident.getLongitude());
        response.setTitle(incident.getTitle());
        response.setReportCount(reportRepository.countByIncidentIdAndDeletedAtIsNull(incident.getId()));
        response.setExpiresAt(incident.getExpiresAt());
        return response;
    }
}
