package com.kieumai.notifier.persistence.repository;

import com.kieumai.notifier.business.model.IncidentAlert;
import com.kieumai.notifier.business.model.NearbyIncident;

import java.time.Instant;
import java.util.List;

public interface IncidentAlertRepository {

    List<IncidentAlert> findActiveNotAlerted();

    boolean markAlerted(long incidentId, Instant alertedAt);

    List<NearbyIncident> findActiveInBox(double minLatitude, double maxLatitude,
                                         double minLongitude, double maxLongitude);
}
