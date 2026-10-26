package com.kieumai.notifier.config;

import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Component
@Validated
@ConfigurationProperties(prefix = "notifier.report")
public class ReportGroupingProperties {
    @Min(1)
    private long groupingRadiusMeters = 500;
    @Min(1)
    private long incidentLifetimeSeconds = 14400;

    public long getGroupingRadiusMeters() { return groupingRadiusMeters; }
    public void setGroupingRadiusMeters(long value) { groupingRadiusMeters = value; }
    public long getIncidentLifetimeSeconds() { return incidentLifetimeSeconds; }
    public void setIncidentLifetimeSeconds(long value) { incidentLifetimeSeconds = value; }
}
