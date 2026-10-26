package com.kieumai.notifier.business.model;

public record IncidentAlert(
        long incidentId,
        long wardId,
        String type,
        String title
) {
}
