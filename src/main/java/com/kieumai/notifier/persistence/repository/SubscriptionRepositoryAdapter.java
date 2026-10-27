package com.kieumai.notifier.persistence.repository;

import com.kieumai.notifier.business.model.Subscription;
import com.kieumai.notifier.persistence.entity.*;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import java.time.ZoneOffset;
import java.util.List;

@Repository
@RequiredArgsConstructor
@Transactional
public class SubscriptionRepositoryAdapter implements SubscriptionRepository {
    private final JpaSubscriptionRepository repository;
    private final EntityManager entityManager;

    @Override
    public Subscription save(Subscription subscription) {
        SubscriptionEntity entity = new SubscriptionEntity();
        entity.setId(subscription.id());
        entity.setUser(entityManager.getReference(User.class, subscription.userId()));
        entity.setWard(entityManager.getReference(WardEntity.class, subscription.wardId()));
        entity.setCreatedAt(subscription.createdAt().atOffset(ZoneOffset.UTC));
        return toModel(repository.saveAndFlush(entity));
    }

    @Override
    public boolean existsByUserIdAndWardId(long userId, long wardId) {
        return repository.existsByUser_IdAndWard_Id(userId, wardId);
    }

    @Override
    public List<Subscription> findByUserId(long userId) {
        return repository.findByUser_IdOrderByIdAsc(userId).stream().map(this::toModel).toList();
    }

    @Override
    public List<Long> findUserIdsByWardId(long wardId) {
        return repository.findUserIdsByWardId(wardId);
    }

    @Override
    public boolean deleteByUserIdAndWardId(long userId, long wardId) {
        return repository.deleteByUser_IdAndWard_Id(userId, wardId) > 0;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Long> findWardIdsByUserId(Long userId) {
        return repository.findWardIdsByUserId(userId);
    }

    private Subscription toModel(SubscriptionEntity entity) {
        return new Subscription(entity.getId(), entity.getUser().getId(),
                entity.getWard().getId(), entity.getCreatedAt().toInstant());
    }
}
