package com.kieumai.notifier.persistence.repository;

import com.kieumai.notifier.business.model.Ward;

import java.util.List;

public interface WardCatalog {
    List<Ward> findAll();
}
