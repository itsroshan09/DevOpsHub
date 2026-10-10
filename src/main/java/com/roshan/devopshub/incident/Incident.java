package com.roshan.devopshub.incident;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "incidents")
public class Incident {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private IncidentSeverity severity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private IncidentStatus status = IncidentStatus.OPEN;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "assigned_to",
            foreignKey = @ForeignKey(name = "fk_incidents_assigned_user")
    )
    private com.roshan.devopshub.user.User assignee;

    protected Incident() {
        // Required by JPA.
    }

    public Incident(
            String title,
            String description,
            IncidentSeverity severity
    ) {
        this.title = title;
        this.description = description;
        this.severity = severity;
    }

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public IncidentSeverity getSeverity() {
        return severity;
    }

    public IncidentStatus getStatus() {
        return status;
    }

    public void transitionTo(IncidentStatus newStatus) {
        this.status = newStatus;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public com.roshan.devopshub.user.User getAssignee() {
        return assignee;
    }

    public void assignTo(com.roshan.devopshub.user.User assignee) {
        this.assignee = assignee;
    }

}
