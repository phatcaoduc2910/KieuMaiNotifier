package com.kieumai.notifier.presentation.dto;

import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Getter
@Setter
public class IncidentDetailDto {
    private Long id;
    private Long wardId;
    private String type;
    private String status;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private String title;
    private long reportCount;
    private OffsetDateTime alertedAt;
    private OffsetDateTime expiresAt;
    private OffsetDateTime createdAt;
}
