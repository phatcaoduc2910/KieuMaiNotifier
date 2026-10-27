package com.kieumai.notifier.business.model;

import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Getter
@Setter
public class Report {
    private Long id;
    private Long userId;
    private Long incidentId;
    private Long wardId;
    private String type;
    private String description;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private OffsetDateTime observedAt;
    private OffsetDateTime createdAt;
    private OffsetDateTime deletedAt;
}
