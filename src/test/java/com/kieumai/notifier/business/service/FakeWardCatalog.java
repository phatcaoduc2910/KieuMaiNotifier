package com.kieumai.notifier.business.service;

import com.kieumai.notifier.business.model.Ward;
import com.kieumai.notifier.persistence.repository.WardCatalog;

import java.util.ArrayList;
import java.util.List;

class FakeWardCatalog implements WardCatalog {

    private final List<Ward> wards = new ArrayList<>();

    void add(long id, String name, String provinceName) {
        wards.add(new Ward(id, "W" + id, name, provinceName));
    }

    @Override
    public List<Ward> findAll() {
        return List.copyOf(wards);
    }
}
