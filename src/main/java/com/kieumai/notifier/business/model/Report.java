package com.kieumai.notifier.business.model;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class Report {

    private Long id;
    private Long userId;
    private Long incidentId;
    private Long wardId;

    private String type;
    private String description;
    private String locationDescription;
    private List<String> imageUrls = new ArrayList<>();
    private BigDecimal latitude;
    private BigDecimal longitude;

    private OffsetDateTime observedAt;
    private OffsetDateTime createdAt;
    private OffsetDateTime deletedAt;
}
