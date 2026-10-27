package com.kieumai.notifier.business.service;

import com.kieumai.notifier.business.exception.ReportException;
import com.kieumai.notifier.persistence.entity.IncidentEntity;
import com.kieumai.notifier.persistence.repository.*;
import com.kieumai.notifier.presentation.dto.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static com.kieumai.notifier.business.exception.ReportException.Code.*;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class IncidentService {
    private final IncidentRepository incidentRepository;
    private final ReportRepository reportRepository;
    private final WardRepository wardRepository;

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
