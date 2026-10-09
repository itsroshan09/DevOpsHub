package com.roshan.devopshub.incident;

import java.time.Instant;

public record IncidentResponse(
        Long id,
        String title,
        String description,
        IncidentSeverity severity,
        IncidentStatus status,
        Instant createdAt,
        Instant updatedAt
) {
    public static IncidentResponse from(Incident incident) {
        return new IncidentResponse(
                incident.getId(),
                incident.getTitle(),
                incident.getDescription(),
                incident.getSeverity(),
                incident.getStatus(),
                incident.getCreatedAt(),
                incident.getUpdatedAt()
        );
    }
}
