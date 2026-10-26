package com.kieumai.notifier.persistence.jpa;

import com.kieumai.notifier.business.exception.AlreadySubscribedException;
import com.kieumai.notifier.business.model.Subscription;
import com.kieumai.notifier.persistence.entity.SubscriptionEntity;
import com.kieumai.notifier.persistence.repository.SubscriptionRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class SubscriptionRepositoryImpl implements SubscriptionRepository {

    private final SubscriptionJpaRepository jpaRepository;

    public SubscriptionRepositoryImpl(SubscriptionJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Subscription save(Subscription subscription) {
        SubscriptionEntity entity = new SubscriptionEntity(
                subscription.userId(), subscription.wardId(), subscription.createdAt());
        try {
            return toModel(jpaRepository.saveAndFlush(entity));
        } catch (DataIntegrityViolationException e) {
            throw new AlreadySubscribedException(subscription.userId(), subscription.wardId());
        }
    }

    @Override
    public boolean existsByUserIdAndWardId(long userId, long wardId) {
        return jpaRepository.existsByUserIdAndWardId(userId, wardId);
    }

    @Override
    public List<Subscription> findByUserId(long userId) {
        return jpaRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(SubscriptionRepositoryImpl::toModel)
                .toList();
    }

    @Override
    public List<Long> findUserIdsByWardId(long wardId) {
        return jpaRepository.findUserIdsByWardId(wardId);
    }

    @Override
    public boolean deleteByUserIdAndWardId(long userId, long wardId) {
        return jpaRepository.deleteByUserIdAndWardId(userId, wardId) > 0;
    }

    private static Subscription toModel(SubscriptionEntity entity) {
        return new Subscription(entity.getId(), entity.getUserId(), entity.getWardId(), entity.getCreatedAt());
    }
}
