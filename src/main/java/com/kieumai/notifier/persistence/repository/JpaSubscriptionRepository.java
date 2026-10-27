package com.kieumai.notifier.persistence.repository;

import com.kieumai.notifier.persistence.entity.SubscriptionEntity;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface JpaSubscriptionRepository extends JpaRepository<SubscriptionEntity, Long> {
    boolean existsByUser_IdAndWard_Id(long userId, long wardId);
    List<SubscriptionEntity> findByUser_IdOrderByIdAsc(long userId);
    long deleteByUser_IdAndWard_Id(long userId, long wardId);

    @Query("select s.ward.id from SubscriptionEntity s where s.user.id = :userId order by s.ward.id")
    List<Long> findWardIdsByUserId(@Param("userId") Long userId);

    @Query("select s.user.id from SubscriptionEntity s where s.ward.id = :wardId")
    List<Long> findUserIdsByWardId(@Param("wardId") long wardId);
}
