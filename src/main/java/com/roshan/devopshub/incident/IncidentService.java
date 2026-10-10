package com.roshan.devopshub.incident;

import com.roshan.devopshub.incident.Incident;
import com.roshan.devopshub.incident.IncidentNotFoundException;
import com.roshan.devopshub.incident.IncidentRepository;
import com.roshan.devopshub.incident.IncidentSeverity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class IncidentService {

    private final IncidentRepository incidentRepository;

    public IncidentService(IncidentRepository incidentRepository) {
        this.incidentRepository = incidentRepository;
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
}
