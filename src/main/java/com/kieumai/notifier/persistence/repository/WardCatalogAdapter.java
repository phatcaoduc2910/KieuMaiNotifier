package com.kieumai.notifier.persistence.repository;

import com.kieumai.notifier.business.model.Ward;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public class WardCatalogAdapter implements WardCatalog {

    private final WardRepository wardRepository;

    public WardCatalogAdapter(WardRepository wardRepository) {
        this.wardRepository = wardRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Ward> findAll() {
        return wardRepository.findAll().stream()
                .map(w -> new Ward(w.getId(), w.getCode(), w.getName(), w.getProvinceName()))
                .toList();
    }
}
