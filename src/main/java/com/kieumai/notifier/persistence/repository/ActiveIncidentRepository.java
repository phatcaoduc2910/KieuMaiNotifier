package com.kieumai.notifier.persistence.repository;

import com.kieumai.notifier.business.model.ActiveIncident;

import java.util.Collection;
import java.util.List;

public interface ActiveIncidentRepository {
    List<ActiveIncident> findActiveByWardIds(Collection<Long> wardIds);
}
