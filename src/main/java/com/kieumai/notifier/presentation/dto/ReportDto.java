package com.kieumai.notifier.presentation.dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

@Getter
@Setter
public class ReportDto {

    private Long id;
    private Long userId;
    private Long incidentId;
    private Long wardId;
    private String type;
    private String description;
    private String locationDescription;
    private List<String> imageUrls;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private OffsetDateTime observedAt;
    private OffsetDateTime createdAt;

}
