package com.kieumai.notifier.business.service;

import com.kieumai.notifier.business.model.ActiveIncident;
import com.kieumai.notifier.business.model.Subscription;
import com.kieumai.notifier.persistence.repository.ActiveIncidentRepository;
import com.kieumai.notifier.persistence.repository.SubscriptionRepository;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class FeedService {

    private static final Comparator<ActiveIncident> NEWEST_FIRST = Comparator
            .comparing(ActiveIncident::createdAt, Comparator.nullsLast(Comparator.<Instant>reverseOrder()))
            .thenComparing(ActiveIncident::incidentId, Comparator.reverseOrder());

    private final SubscriptionRepository subscriptionRepository;
    private final ActiveIncidentRepository activeIncidentRepository;

    public FeedService(SubscriptionRepository subscriptionRepository,
                       ActiveIncidentRepository activeIncidentRepository) {
        this.subscriptionRepository = subscriptionRepository;
        this.activeIncidentRepository = activeIncidentRepository;
    }

    public List<ActiveIncident> getFeed(long userId) {
        Set<Long> wardIds = subscriptionRepository.findByUserId(userId).stream()
                .map(Subscription::wardId)
                .collect(Collectors.toSet());
        if (wardIds.isEmpty()) {
            return List.of();
        }
        return activeIncidentRepository.findActiveByWardIds(wardIds).stream()
                .sorted(NEWEST_FIRST)
                .toList();
    }
}
