package com.kieumai.notifier.business.model;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Getter
@Setter
public class Incident {
    private Long id;
    private Long wardId;
    private String type;
    private String title;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private String status;
    private long reportCount;
    private OffsetDateTime startedAt;
    private OffsetDateTime expiresAt;
    private OffsetDateTime alertedAt;
    private OffsetDateTime createdAt;
}
