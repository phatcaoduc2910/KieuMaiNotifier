package com.kieumai.notifier.persistence.repository;

import com.kieumai.notifier.business.model.Subscription;

import java.util.List;

public interface SubscriptionRepository {
    Subscription save(Subscription subscription);

    boolean existsByUserIdAndWardId(long userId, long wardId);

    List<Subscription> findByUserId(long userId);

    List<Long> findUserIdsByWardId(long wardId);

    boolean deleteByUserIdAndWardId(long userId, long wardId);
}
