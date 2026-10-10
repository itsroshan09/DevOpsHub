package com.roshan.devopshub.incident;


import com.roshan.devopshub.audit.AuditEvent;
import com.roshan.devopshub.audit.AuditEventRepository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Comparator;
import com.roshan.devopshub.user.User;
import com.roshan.devopshub.user.UserRepository;
import com.roshan.devopshub.user.UserNotFoundException;
import java.util.List;

@Service
public class IncidentService {

    private final IncidentRepository incidentRepository;
    private final IncidentUpdateRepository incidentUpdateRepository;
    private final AuditEventRepository auditEventRepository;
    private final UserRepository userRepository;

    public IncidentService(
            IncidentRepository incidentRepository,
            IncidentUpdateRepository incidentUpdateRepository,
            AuditEventRepository auditEventRepository, UserRepository userRepository) {

        this.incidentRepository = incidentRepository;
        this.incidentUpdateRepository = incidentUpdateRepository;
        this.auditEventRepository = auditEventRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public Incident createIncident(
            String title,
            String description,
            IncidentSeverity severity) {

        Incident incident = new Incident(title, description, severity);

        Incident savedIncident = incidentRepository.save(incident);

        AuditEvent auditEvent = new AuditEvent(
                savedIncident,
                "INCIDENT_CREATED",
                "Incident created"
        );

        auditEventRepository.save(auditEvent);

        return savedIncident;
    }

    @Transactional(readOnly = true)
    public Incident getIncidentById(Long id) {
        return incidentRepository.findWithAssigneeById(id)
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

    @Transactional(readOnly = true)
    public List<IncidentHistoryItemResponse> getIncidentHistory(
            Long incidentId
    ) {
        // Ensure the incident exists before retrieving its history.
        incidentRepository.findById(incidentId)
                .orElseThrow(() -> new IncidentNotFoundException(incidentId));

        List<IncidentHistoryItemResponse> history =
                new java.util.ArrayList<>();

        incidentUpdateRepository
                .findByIncidentIdOrderByCreatedAtAsc(incidentId)
                .forEach(update -> history.add(
                        new IncidentHistoryItemResponse(
                                "INVESTIGATION_UPDATE",
                                update.getMessage(),
                                update.getCreatedAt()
                        )
                ));

        auditEventRepository
                .findByIncidentIdOrderByCreatedAtAscIdAsc(incidentId)
                .forEach(event -> history.add(
                        new IncidentHistoryItemResponse(
                                event.getAction(),
                                event.getDetails(),
                                event.getCreatedAt()
                        )
                ));

        history.sort(
                Comparator.comparing(
                                IncidentHistoryItemResponse::createdAt
                        )
                        .thenComparing(
                                IncidentHistoryItemResponse::type
                        )
        );

        return history;
    }


    @Transactional
    public Incident assignIncident(Long incidentId, Long userId) {
        Incident incident = incidentRepository.findById(incidentId)
                .orElseThrow(() -> new IncidentNotFoundException(incidentId));

        User newAssignee = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        User previousAssignee = incident.getAssignee();
        String previousAssigneeName = previousAssignee == null
                ? "Unassigned"
                : previousAssignee.getEmail();

        incident.assignTo(newAssignee);

        Incident savedIncident = incidentRepository.save(incident);

        AuditEvent auditEvent = new AuditEvent(
                savedIncident,
                "INCIDENT_ASSIGNED",
                previousAssigneeName + " -> " + newAssignee.getEmail()
        );

        auditEventRepository.save(auditEvent);

        return savedIncident;
    }



}
