package com.kieumai.notifier.presentation.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;

@Getter
@Setter
public class ReportUpdateDto {
    private Long id;
    private Long reportId;
    private Long userId;
    private String content;
    private OffsetDateTime createdAt;
}
