package com.kieumai.notifier.persistence.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "reports", indexes = {
        @Index(name = "idx_reports_user", columnList = "user_id"),
        @Index(name = "idx_reports_incident", columnList = "incident_id"),
        @Index(name = "idx_reports_group", columnList = "report_group_id")
})
@Getter
@Setter
public class ReportEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "incident_id")
    private IncidentEntity incident;

    @Column(name = "ward_id")
    private Long wardId;

    private String type;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "report_group_id")
    private ReportGroupEntity group;

    @Column(nullable = false, length = 2000)
    private String description;

    @Column(name = "location_description", length = 500)
    private String locationDescription;

    @ElementCollection
    @CollectionTable(name = "report_images", joinColumns = @JoinColumn(name = "report_id"))
    @OrderColumn(name = "image_order")
    @Column(name = "image_url", nullable = false, length = 2048)
    private List<String> imageUrls = new ArrayList<>();

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

    public ReportEntity() {
    }

    @PrePersist
    private void initializeCreatedAt() {
        if (createdAt == null) {
            createdAt = OffsetDateTime.now(ZoneOffset.UTC);
        }
    }
}
