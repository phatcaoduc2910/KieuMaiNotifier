package com.kieumai.notifier.business.model;

import java.time.Instant;

public record NearbyIncident(
        long incidentId,
        long wardId,
        String type,
        String title,
        double latitude,
        double longitude,
        Instant expiresAt
) {
}
