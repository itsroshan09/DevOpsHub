package com.roshan.devopshub.incident;


import com.roshan.devopshub.audit.AuditEvent;
import com.roshan.devopshub.audit.AuditEventRepository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class IncidentService {

    private final IncidentRepository incidentRepository;
    private final IncidentUpdateRepository incidentUpdateRepository;
    private final AuditEventRepository auditEventRepository;

    public IncidentService(
            IncidentRepository incidentRepository,
            IncidentUpdateRepository incidentUpdateRepository,
            AuditEventRepository auditEventRepository) {

        this.incidentRepository = incidentRepository;
        this.incidentUpdateRepository = incidentUpdateRepository;
        this.auditEventRepository = auditEventRepository;
    }

    @Transactional
    public Incident createIncident(
            String title,
            String description,
            IncidentSeverity severity) {

        Incident incident = new Incident(title, description, severity);
        return incidentRepository.save(incident);
    }

    @Transactional(readOnly = true)
    public Incident getIncidentById(Long id) {
        return incidentRepository.findById(id)
                .orElseThrow(() -> new IncidentNotFoundException(id));
    }

    @Transactional(readOnly = true)
    public Page<Incident> getIncidents(Pageable pageable) {
        return incidentRepository.findAll(pageable);
    }

    @Transactional(readOnly = true)
    public Page<Incident> getIncidents(
            IncidentStatus status,
            IncidentSeverity severity,
            Pageable pageable) {

        return incidentRepository.findByFilters(status, severity, pageable);
    }

    @Transactional
    public Incident transitionStatus(Long id, IncidentStatus newStatus) {
        Incident incident = incidentRepository.findById(id)
                .orElseThrow(() -> new IncidentNotFoundException(id));

        IncidentStatus currentStatus = incident.getStatus();

        boolean isValidTransition =
                (currentStatus == IncidentStatus.OPEN
                        && newStatus == IncidentStatus.IN_PROGRESS)
                        || (currentStatus == IncidentStatus.IN_PROGRESS
                        && newStatus == IncidentStatus.RESOLVED);

        if (!isValidTransition) {
            throw new IllegalArgumentException(
                    "Invalid status transition from "
                            + currentStatus + " to " + newStatus
            );
        }

        incident.transitionTo(newStatus);

        Incident savedIncident = incidentRepository.save(incident);

        AuditEvent auditEvent = new AuditEvent(
                savedIncident,
                "STATUS_CHANGED",
                currentStatus + " -> " + newStatus
        );

        auditEventRepository.save(auditEvent);

        return savedIncident;
    }


    @Transactional
    public IncidentUpdate addUpdate(Long incidentId, String message) {
        Incident incident = incidentRepository.findById(incidentId)
                .orElseThrow(() -> new IncidentNotFoundException(incidentId));

        IncidentUpdate update = new IncidentUpdate(incident, message);

        return incidentUpdateRepository.save(update);
    }


    @Transactional(readOnly = true)
    public List<IncidentUpdate> getIncidentUpdates(Long incidentId) {
        incidentRepository.findById(incidentId)
                .orElseThrow(() -> new IncidentNotFoundException(incidentId));

        return incidentUpdateRepository
                .findByIncidentIdOrderByCreatedAtAsc(incidentId);
    }

}
