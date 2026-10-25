package com.kieumai.notifier.persistence.repository;

import com.kieumai.notifier.persistence.entity.ReportGroupingLock;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ReportGroupingLockRepository extends JpaRepository<ReportGroupingLock, String> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select l from ReportGroupingLock l where l.key = :key")
    Optional<ReportGroupingLock> findByKeyForUpdate(@Param("key") String key);
}
