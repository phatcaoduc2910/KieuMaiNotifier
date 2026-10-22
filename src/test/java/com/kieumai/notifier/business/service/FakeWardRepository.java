package com.kieumai.notifier.business.service;

import com.kieumai.notifier.persistence.repository.WardRepository;

import java.util.Set;

class FakeWardRepository implements WardRepository {

    private final Set<Long> existingWardIds;

    FakeWardRepository(Long... wardIds) {
        this.existingWardIds = Set.of(wardIds);
    }

    @Override
    public boolean existsById(long wardId) {
        return existingWardIds.contains(wardId);
    }
}
