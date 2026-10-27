package com.kieumai.notifier.presentation.dto;

import com.kieumai.notifier.business.model.ActiveIncident;

import java.time.Instant;

public record FeedItemResponse(
        long incidentId,
        long wardId,
        String type,
        String title,
        Double latitude,
        Double longitude,
        Instant createdAt,
        Instant expiresAt
) {

    public static FeedItemResponse from(ActiveIncident incident) {
        return new FeedItemResponse(
                incident.incidentId(),
                incident.wardId(),
                incident.type(),
                incident.title(),
                incident.latitude(),
                incident.longitude(),
                incident.createdAt(),
                incident.expiresAt());
    }
}
