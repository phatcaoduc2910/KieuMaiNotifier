package com.kieumai.notifier.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;

@Entity
@Table(name = "subscriptions",
        uniqueConstraints = @UniqueConstraint(name = "uk_subscriptions_user_ward", columnNames = {"user_id", "ward_id"}),
        indexes = @Index(name = "idx_subscriptions_ward", columnList = "ward_id"))
public class SubscriptionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private long userId;

    @Column(name = "ward_id", nullable = false)
    private long wardId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected SubscriptionEntity() {
    }

    public SubscriptionEntity(long userId, long wardId, Instant createdAt) {
        this.userId = userId;
        this.wardId = wardId;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public long getUserId() {
        return userId;
    }

    public long getWardId() {
        return wardId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
