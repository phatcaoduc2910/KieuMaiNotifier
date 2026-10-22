package com.kieumai.notifier.business.service;

import com.kieumai.notifier.business.exception.AlreadySubscribedException;
import com.kieumai.notifier.business.exception.SubscriptionNotFoundException;
import com.kieumai.notifier.business.exception.WardNotFoundException;
import com.kieumai.notifier.business.model.Subscription;
import com.kieumai.notifier.persistence.repository.SubscriptionRepository;
import com.kieumai.notifier.persistence.repository.WardRepository;

import java.time.Instant;
import java.util.List;

public class SubscriptionService {

    private final SubscriptionRepository subscriptionRepository;
    private final WardRepository wardRepository;

    public SubscriptionService(SubscriptionRepository subscriptionRepository,
                               WardRepository wardRepository) {
        this.subscriptionRepository = subscriptionRepository;
        this.wardRepository = wardRepository;
    }

    public Subscription subscribe(long userId, long wardId) {
        if (!wardRepository.existsById(wardId)) {
            throw new WardNotFoundException(wardId);
        }
        if (subscriptionRepository.existsByUserIdAndWardId(userId, wardId)) {
            throw new AlreadySubscribedException(userId, wardId);
        }
        Subscription newSubscription = new Subscription(null, userId, wardId, Instant.now());
        return subscriptionRepository.save(newSubscription);
    }

    public void unsubscribe(long userId, long wardId) {
        boolean deleted = subscriptionRepository.deleteByUserIdAndWardId(userId, wardId);
        if (!deleted) {
            throw new SubscriptionNotFoundException(userId, wardId);
        }
    }

    public List<Subscription> getSubscriptions(long userId) {
        return subscriptionRepository.findByUserId(userId);
    }
}
