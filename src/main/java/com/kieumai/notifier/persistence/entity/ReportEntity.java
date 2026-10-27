package com.kieumai.notifier.persistence.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Entity
@Table(name = "reports")
@Getter
@Setter
public class ReportEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "incident_id", nullable = false)
    private IncidentEntity incident;
    @Column(nullable = false, columnDefinition = "text")
    private String description;
    @Column(nullable = false, precision = 10, scale = 7)
    private BigDecimal latitude;
    @Column(nullable = false, precision = 10, scale = 7)
    private BigDecimal longitude;
    @Column(name = "observed_at", nullable = false)
    private OffsetDateTime observedAt;
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;
    @Column(name = "deleted_at")
    private OffsetDateTime deletedAt;

    @PrePersist
    void initializeCreatedAt() {
        if (createdAt == null) createdAt = OffsetDateTime.now(ZoneOffset.UTC);
    }
}
