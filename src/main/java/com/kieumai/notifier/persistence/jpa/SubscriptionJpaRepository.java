package com.kieumai.notifier.persistence.jpa;

import com.kieumai.notifier.persistence.entity.SubscriptionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface SubscriptionJpaRepository extends JpaRepository<SubscriptionEntity, Long> {

    boolean existsByUserIdAndWardId(long userId, long wardId);

    List<SubscriptionEntity> findByUserIdOrderByCreatedAtDesc(long userId);

    @Query("select s.userId from SubscriptionEntity s where s.wardId = :wardId")
    List<Long> findUserIdsByWardId(@Param("wardId") long wardId);

    @Transactional
    @Modifying
    @Query("delete from SubscriptionEntity s where s.userId = :userId and s.wardId = :wardId")
    int deleteByUserIdAndWardId(@Param("userId") long userId, @Param("wardId") long wardId);
}
