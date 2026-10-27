package com.kieumai.notifier.persistence.repository;

import com.kieumai.notifier.persistence.entity.WardEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.Optional;

public interface WardRepository extends JpaRepository<WardEntity, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select w from WardEntity w where w.id = :id")
    Optional<WardEntity> findByIdForUpdate(@Param("id") Long id);
}
