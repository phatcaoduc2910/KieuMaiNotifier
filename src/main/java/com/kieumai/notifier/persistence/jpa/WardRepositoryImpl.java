package com.kieumai.notifier.persistence.jpa;

import com.kieumai.notifier.persistence.repository.WardRepository;
import org.springframework.stereotype.Repository;

@Repository
public class WardRepositoryImpl implements WardRepository {

    private final WardJpaRepository jpaRepository;

    public WardRepositoryImpl(WardJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public boolean existsById(long wardId) {
        return jpaRepository.existsById(wardId);
    }
}
