package com.kieumai.notifier.business.model;

import java.time.Instant;

public record ActiveIncident(
        long incidentId,
        long wardId,
        String type,
        String title,
        Double latitude,
        Double longitude,
        Instant createdAt,
        Instant expiresAt
) {
}
