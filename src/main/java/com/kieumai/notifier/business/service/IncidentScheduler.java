package com.kieumai.notifier.business.service;

import com.kieumai.notifier.persistence.repository.IncidentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

@Component
@RequiredArgsConstructor
public class IncidentScheduler {
    private final IncidentRepository incidentRepository;

    @Scheduled(fixedRate = 300000)
    @Transactional
    public void autoExpireIncidents() {
        incidentRepository.findByStatusInAndExpiresAtBefore(
                        List.of("PENDING", "ACTIVE", "ALERTED"), OffsetDateTime.now(ZoneOffset.UTC))
                .forEach(incident -> incident.setStatus("EXPIRED"));
    }
}
