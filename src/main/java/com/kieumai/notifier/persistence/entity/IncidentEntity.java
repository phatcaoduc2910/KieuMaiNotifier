package com.kieumai.notifier.persistence.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity
@Table(name = "Incidents")
public class IncidentEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;
    private long wardId;
    private String type;
    private String title;
    private BigDecimal latitude;
    private BigDecimal longtitude;
    private String status;
    private OffsetDateTime startedAt;
    private OffsetDateTime expires;
    private OffsetDateTime alertedAt;
    private OffsetDateTime createdAt;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getWard_id() {
        return wardId;
    }

    public void setWard_id(long ward_id) {
        this.wardId = ward_id;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public BigDecimal getLatitude() {
        return latitude;
    }

    public void setLatitude(BigDecimal latitude) {
        this.latitude = latitude;
    }

    public BigDecimal getLongtitude() {
        return longtitude;
    }

    public void setLongtitude(BigDecimal longtitude) {
        this.longtitude = longtitude;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public OffsetDateTime getStarted_at() {
        return startedAt;
    }

    public void setStarted_at(OffsetDateTime started_at) {
        this.startedAt = started_at;
    }

    public OffsetDateTime getExpires() {
        return expires;
    }

    public void setExpires(OffsetDateTime expires) {
        this.expires = expires;
    }

    public OffsetDateTime getAlerted_at() {
        return alertedAt;
    }

    public void setAlerted_at(OffsetDateTime alerted_at) {
        this.alertedAt = alerted_at;
    }

    public OffsetDateTime getCreated_at() {
        return createdAt;
    }

    public void setCreated_at(OffsetDateTime created_at) {
        this.createdAt = created_at;
    }
}
