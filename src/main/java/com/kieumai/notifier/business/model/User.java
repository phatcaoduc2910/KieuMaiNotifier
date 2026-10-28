package com.kieumai.notifier.business.model;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Getter
@Setter
public class User {
    private Long id;
    private String username;
    private String displayName;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private OffsetDateTime createdAt;
}
