package com.kieumai.notifier.persistence.jpa;

import com.kieumai.notifier.persistence.entity.WardEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WardJpaRepository extends JpaRepository<WardEntity, Long> {
}
