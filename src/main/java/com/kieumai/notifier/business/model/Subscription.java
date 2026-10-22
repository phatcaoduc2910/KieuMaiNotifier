package com.kieumai.notifier.business.model;

import java.time.Instant;

public record Subscription(
        Long id,
        long userId,
        long wardId,
        Instant createdAt
) {

}
