package com.kieumai.notifier.persistence.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;

@Entity
@Table(name = "IncidentConfirmations")
@Getter
@Setter
@NoArgsConstructor
public class IncidentConfirmationEntity {
    @EmbeddedId
    private IncidentConfirmationId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("incidentId")
    private IncidentEntity incident;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("userId")
    private UserEntity user;

    private OffsetDateTime created_at;
}
