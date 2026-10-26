package com.kieumai.notifier.business.model;

import java.time.Instant;

public record Notification(
        Long id,
        long userId,
        long incidentId,
        String title,
        String message,
        Instant createdAt,
        Instant readAt
) {
}
