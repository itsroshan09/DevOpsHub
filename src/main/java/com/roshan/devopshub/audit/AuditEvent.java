
package com.roshan.devopshub.audit;

import com.roshan.devopshub.incident.Incident;
import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(
        name = "audit_events",
        indexes = {
                @Index(
                        name = "idx_audit_events_incident_created_at",
                        columnList = "incident_id, created_at, id"
                )
        }
)
public class AuditEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "incident_id", nullable = false)
    private Incident incident;

    @Column(nullable = false, length = 50)
    private String action;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String details;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected AuditEvent() {
        // Required by JPA.
    }

    public AuditEvent(Incident incident, String action, String details) {
        if (incident == null) {
            throw new IllegalArgumentException("Incident is required");
        }
        if (action == null || action.isBlank() || action.length() > 50) {
            throw new IllegalArgumentException(
                    "Action must contain between 1 and 50 characters"
            );
        }
        if (details == null || details.isBlank()) {
            throw new IllegalArgumentException("Details are required");
        }

        this.incident = incident;
        this.action = action;
        this.details = details;
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

    public String getAction() {
        return action;
    }

    public String getDetails() {
        return details;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
