package com.kieumai.notifier.business.service;

import com.kieumai.notifier.business.model.Subscription;
import com.kieumai.notifier.persistence.repository.SubscriptionRepository;

import java.util.ArrayList;
import java.util.List;

class InMemorySubscriptionRepository implements SubscriptionRepository {

    private final List<Subscription> rows = new ArrayList<>();
    private long nextId = 1;

    @Override
    public Subscription save(Subscription subscription) {
        Subscription saved = new Subscription(
                nextId++, subscription.userId(), subscription.wardId(), subscription.createdAt());
        rows.add(saved);
        return saved;
    }

    @Override
    public boolean existsByUserIdAndWardId(long userId, long wardId) {
        return rows.stream().anyMatch(s -> s.userId() == userId && s.wardId() == wardId);
    }

    @Override
    public List<Subscription> findByUserId(long userId) {
        return rows.stream().filter(s -> s.userId() == userId).toList();
    }

    @Override
    public List<Long> findUserIdsByWardId(long wardId) {
        return rows.stream().filter(s -> s.wardId() == wardId).map(Subscription::userId).toList();
    }

    @Override
    public List<Long> findWardIdsByUserId(Long userId) {
        return rows.stream().filter(s -> s.userId() == userId)
                .map(Subscription::wardId).sorted().toList();
    }

    @Override
    public boolean deleteByUserIdAndWardId(long userId, long wardId) {
        return rows.removeIf(s -> s.userId() == userId && s.wardId() == wardId);
    }
}
