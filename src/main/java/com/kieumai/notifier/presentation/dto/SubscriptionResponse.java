package com.kieumai.notifier.presentation.dto;

import com.kieumai.notifier.business.model.Subscription;

import java.time.Instant;

public record SubscriptionResponse(long id, long wardId, Instant createdAt) {

    public static SubscriptionResponse from(Subscription subscription) {
        return new SubscriptionResponse(subscription.id(), subscription.wardId(), subscription.createdAt());
    }
}
