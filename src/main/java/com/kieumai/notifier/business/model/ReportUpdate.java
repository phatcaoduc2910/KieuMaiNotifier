package com.kieumai.notifier.business.model;

import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;

@Getter
@Setter
public class ReportUpdate {
    private Long id;
    private Long reportId;
    private Long userId;
    private String content;
    private OffsetDateTime createdAt;
}
