package com.kieumai.notifier.persistence.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;

@Entity
@Table(name = "incident_confirmations")
@Getter
@Setter
@NoArgsConstructor
public class IncidentConfirmationEntity {
    @EmbeddedId
    private IncidentConfirmationId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("incidentId")
    @JoinColumn(name = "incident_id")
    private IncidentEntity incident;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("userId")
    @JoinColumn(name = "user_id")
    private UserEntity user;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    public IncidentConfirmationEntity(IncidentEntity incident, UserEntity user) {
        this.id = new IncidentConfirmationId(incident.getId(), user.getId());
        this.incident = incident;
        this.user = user;
    }
}
