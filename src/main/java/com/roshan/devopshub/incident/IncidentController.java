
package com.roshan.devopshub.incident;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import jakarta.validation.Valid;

import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;




@RestController
@RequestMapping("/api/incidents")
public class IncidentController {

    private final IncidentService incidentService;

    public IncidentController(IncidentService incidentService) {
        this.incidentService = incidentService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public IncidentResponse createIncident(
            @Valid @RequestBody CreateIncidentRequest request
    ) {
        Incident incident = incidentService.createIncident(
                request.title(),
                request.description(),
                request.severity()
        );

        return IncidentResponse.from(incident);
    }

    @GetMapping("/{id}")
    public IncidentResponse getIncidentById(@PathVariable Long id) {
        Incident incident = incidentService.getIncidentById(id);
        return IncidentResponse.from(incident);
    }

    @GetMapping
    public Page<IncidentResponse> getIncidents(
            @RequestParam(required = false) IncidentStatus status,
            @RequestParam(required = false) IncidentSeverity severity,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        if (page < 0) {
            throw new IllegalArgumentException(
                    "Page number must be zero or greater");
        }

        if (size < 1 || size > 100) {
            throw new IllegalArgumentException(
                    "Page size must be between 1 and 100");
        }

        PageRequest pageable = PageRequest.of(
                page,
                size,
                Sort.by(Sort.Direction.DESC, "createdAt")
        );

        return incidentService.getIncidents(status, severity, pageable)
                .map(IncidentResponse::from);
    }


    @PostMapping("/{id}/transitions")
    public IncidentResponse transitionIncident(
            @PathVariable Long id,
            @Valid @RequestBody TransitionIncidentRequest request) {

        Incident incident = incidentService.transitionStatus(
                id,
                request.status()
        );

        return IncidentResponse.from(incident);
    }


    @PostMapping("/{id}/updates")
    public ResponseEntity<IncidentUpdateResponse> addIncidentUpdate(
            @PathVariable Long id,
            @Valid @RequestBody CreateIncidentUpdateRequest request) {

        IncidentUpdate update = incidentService.addUpdate(
                id,
                request.message()
        );

        IncidentUpdateResponse response = new IncidentUpdateResponse(
                update.getId(),
                update.getIncident().getId(),
                update.getMessage(),
                update.getCreatedAt()
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }


    @GetMapping("/{id}/updates")
    public ResponseEntity<List<IncidentUpdateResponse>> getIncidentUpdates(
            @PathVariable Long id) {

        List<IncidentUpdateResponse> updates =
                incidentService.getIncidentUpdates(id)
                        .stream()
                        .map(update -> new IncidentUpdateResponse(
                                update.getId(),
                                update.getIncident().getId(),
                                update.getMessage(),
                                update.getCreatedAt()
                        ))
                        .toList();

        return ResponseEntity.ok(updates);
    }

    @GetMapping("/{id}/history")
    public ResponseEntity<List<IncidentHistoryItemResponse>> getIncidentHistory(
            @PathVariable Long id
    ) {
        List<IncidentHistoryItemResponse> history =
                incidentService.getIncidentHistory(id);

        return ResponseEntity.ok(history);
    }

}
