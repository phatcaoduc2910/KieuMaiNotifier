package com.kieumai.notifier.business.service;

import com.kieumai.notifier.persistence.entity.IncidentEntity;
import com.kieumai.notifier.persistence.repository.IncidentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class IncidentScheduler {

    private final IncidentRepository incidentRepository;

    @Scheduled(fixedRate = 300000)
    @Transactional
    public void autoExpireIncidents() {
        log.info("Bắt đầu tiến trình kiểm tra sự cố hết hạn...");

        OffsetDateTime now = OffsetDateTime.now();
        List<IncidentEntity> expiredIncidents = incidentRepository.findByStatusAndExpiresAtBefore("ACTIVE", now);

        if (!expiredIncidents.isEmpty()) {
            for (IncidentEntity incident : expiredIncidents) {
                incident.setStatus("EXPIRED");
            }
            incidentRepository.saveAll(expiredIncidents);
            log.info("Đã tự động chuyển {} sự cố hết hạn sang trạng thái EXPIRED.", expiredIncidents.size());
        }
    }
}