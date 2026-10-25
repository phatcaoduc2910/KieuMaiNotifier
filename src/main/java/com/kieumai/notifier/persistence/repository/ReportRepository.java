package com.kieumai.notifier.persistence.repository;

import com.kieumai.notifier.persistence.entity.ReportEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import java.util.Optional;

public interface ReportRepository extends JpaRepository<ReportEntity, Long>, JpaSpecificationExecutor<ReportEntity> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from ReportEntity r where r.id = :id and r.deletedAt is null")
    Optional<ReportEntity> findVisibleByIdForUpdate(@Param("id") Long id);
}
