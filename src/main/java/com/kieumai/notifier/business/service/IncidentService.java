package com.kieumai.notifier.business.service;

import com.kieumai.notifier.persistence.entity.IncidentConfirmationEntity;
import com.kieumai.notifier.persistence.entity.IncidentEntity;
import com.kieumai.notifier.persistence.entity.User;
import com.kieumai.notifier.persistence.repository.IncidentConfirmationRepository;
import com.kieumai.notifier.persistence.repository.IncidentRepository;
import com.kieumai.notifier.persistence.repository.UserRepository;
import com.kieumai.notifier.presentation.dto.IncidentDetailDto;
import com.kieumai.notifier.presentation.dto.IncidentResolveDto;
import com.kieumai.notifier.presentation.dto.IncidentResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class IncidentService {

    // THÊM TỪ KHÓA 'final' CHO TẤT CẢ CÁC REPOSITORY ĐỂ LOMBOK TIÊM DEPENDENCY
    private final IncidentRepository incidentRepository;
    private final IncidentConfirmationRepository confirmationRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<IncidentResponseDto> getIncidents(Long wardId, String type) {
        List<IncidentEntity> entities;

        if (wardId != null && type != null) {
            entities = incidentRepository.findByWardIdAndType(wardId, type);
        } else if (wardId != null) {
            entities = incidentRepository.findByWardId(wardId);
        } else if (type != null) {
            entities = incidentRepository.findByType(type);
        } else {
            entities = incidentRepository.findAll();
        }

        return entities.stream().map(this::mapToResponseDto).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public IncidentDetailDto getIncidentDetail(Long incidentId, Long currentUserId) {
        IncidentEntity incident = incidentRepository.findById(incidentId)
                .orElseThrow(() -> new RuntimeException("Sự cố không tồn tại với ID: " + incidentId));

        boolean isConfirmedByMe = false;
        if (currentUserId != null) {
            isConfirmedByMe = confirmationRepository.existsByIdIncidentIdAndIdUserId(incidentId, currentUserId);
        }

        return mapToDetailDto(incident, isConfirmedByMe);
    }

    @Transactional
    public void confirmIncident(Long incidentId, Long userId) {
        IncidentEntity incident = incidentRepository.findById(incidentId)
                .orElseThrow(() -> new RuntimeException("Sự cố không tồn tại với ID: " + incidentId));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Người dùng không tồn tại với ID: " + userId));

        if (confirmationRepository.existsByIdIncidentIdAndIdUserId(incidentId, userId)) {
            return;
        }

        IncidentConfirmationEntity confirmation = new IncidentConfirmationEntity(incident, user);
        confirmationRepository.save(confirmation);

        long confirmCount = confirmationRepository.countByIdIncidentId(incidentId);

        if ("PENDING".equalsIgnoreCase(incident.getStatus()) && confirmCount >= 3) {
            incident.setStatus("ACTIVE");
            incident.setExpiresAt(OffsetDateTime.now().plusHours(4));
            incidentRepository.save(incident);
        }
    }

    @Transactional
    public void unconfirmIncident(Long incidentId, Long userId) {
        if (confirmationRepository.existsByIdIncidentIdAndIdUserId(incidentId, userId)) {
            confirmationRepository.deleteByIdIncidentIdAndUserId(incidentId, userId);
        }
    }

    @Transactional
    public void resolveIncidentEarly(Long incidentId, IncidentResolveDto dto, Long userId) {
        IncidentEntity incident = incidentRepository.findById(incidentId)
                .orElseThrow(() -> new RuntimeException("Sự cố không tồn tại với ID: " + incidentId));

        incident.setStatus("RESOLVED");
        incident.setExpiresAt(OffsetDateTime.now());
        incidentRepository.save(incident);
    }

    private IncidentResponseDto mapToResponseDto(IncidentEntity entity) {
        IncidentResponseDto dto = new IncidentResponseDto();
        dto.setId(entity.getId());
        dto.setWardId(entity.getWardId());
        dto.setType(entity.getType());
        dto.setStatus(entity.getStatus());
        dto.setLatitude(entity.getLatitude());
        dto.setLongitude(entity.getLongitude());
        dto.setTitle(entity.getTitle());
        dto.setConfirmationCount(confirmationRepository.countByIdIncidentId(entity.getId()));
        dto.setExpiresAt(entity.getExpiresAt());
        return dto;
    }

    private IncidentDetailDto mapToDetailDto(IncidentEntity entity, boolean isConfirmedByMe) {
        IncidentDetailDto dto = new IncidentDetailDto();
        dto.setId(entity.getId());
        dto.setWardId(entity.getWardId());
        dto.setType(entity.getType());
        dto.setStatus(entity.getStatus());
        dto.setLatitude(entity.getLatitude());
        dto.setLongitude(entity.getLongitude());
        dto.setTitle(entity.getTitle());
        dto.setConfirmationCount(confirmationRepository.countByIdIncidentId(entity.getId()));
        dto.setConfirmedByMe(isConfirmedByMe);
        dto.setAlertedAt(entity.getAlertedAt());
        dto.setExpiresAt(entity.getExpiresAt());
        dto.setCreatedAt(entity.getCreatedAt());
        return dto;
    }
}
