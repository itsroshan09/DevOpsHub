
package com.roshan.devopshub.incident;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(
        name = "incident_updates",
        indexes = {
                @Index(
                        name = "idx_incident_updates_incident_created_at",
                        columnList = "incident_id, created_at"
                )
        }
)
public class IncidentUpdate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "incident_id", nullable = false)
    private Incident incident;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String message;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected IncidentUpdate() {
        // Required by JPA.
    }

    public IncidentUpdate(Incident incident, String message) {
        if (incident == null) {
            throw new IllegalArgumentException(
                    "Incident is required"
            );
        }

        if (message == null || message.isBlank()) {
            throw new IllegalArgumentException(
                    "Update message is required"
            );
        }

        this.incident = incident;
        this.message = message;
    }

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    public Long getId() {
        return id;
    }

    public Incident getIncident() {
        return incident;
    }

    public String getMessage() {
        return message;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
