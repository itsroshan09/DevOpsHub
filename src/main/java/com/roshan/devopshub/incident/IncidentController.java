
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
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;

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
            @AuthenticationPrincipal String actorId,
            @Valid @RequestBody CreateIncidentRequest request
    ) {
        Incident incident = incidentService.createIncident(
                Long.parseLong(actorId),
                request.title(),
                request.description(),
                request.severity(),
                request.serviceId()
        );
        return IncidentResponse.from(incident);
    }

    @GetMapping("/{id}")
    public IncidentResponse getIncidentById(@PathVariable Long id) {
        Incident incident = incidentService.getIncidentById(id);
        return IncidentResponse.from(incident);
    }

    @PutMapping("/{id}/assignee")
    public IncidentResponse assignIncident(
            @PathVariable Long id,
            @AuthenticationPrincipal String actorId,
            @Valid @RequestBody AssignIncidentRequest request
    ) {
        Incident incident = incidentService.assignIncident(
                id,
                request.userId(),
                Long.parseLong(actorId)
        );

        return IncidentResponse.from(incident);
    }


    @GetMapping
    public IncidentPageResponse getIncidents(
            @RequestParam(required = false) IncidentStatus status,
            @RequestParam(required = false) IncidentSeverity severity,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(required = false) String search,
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

        Page<IncidentResponse> result =
                incidentService.getIncidents(status, severity, search, pageable)
                        .map(IncidentResponse::from);

        return IncidentPageResponse.from(result);
    }


    @PostMapping("/{id}/transitions")
    public IncidentResponse transitionIncident(
            @PathVariable Long id,
            @AuthenticationPrincipal String actorId,
            @Valid @RequestBody TransitionIncidentRequest request
    ) {
        Incident incident = incidentService.transitionStatus(
                id,
                request.status(),
                Long.parseLong(actorId)
        );

        return IncidentResponse.from(incident);
    }


    @PostMapping("/{id}/updates")
    public ResponseEntity<IncidentUpdateResponse> addIncidentUpdate(
            @PathVariable Long id,
            @AuthenticationPrincipal String actorId,
            @Valid @RequestBody CreateIncidentUpdateRequest request
    ) {
        IncidentUpdate update = incidentService.addUpdate(
                id,
                request.message(),
                Long.parseLong(actorId)
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
    @PatchMapping("/{id}/severity")
    public IncidentResponse changeSeverity(
            @PathVariable Long id,
            @AuthenticationPrincipal String actorId,
            @Valid @RequestBody ChangeIncidentSeverityRequest request
    ) {
        Incident incident = incidentService.changeSeverity(
                id,
                request.severity(),
                Long.parseLong(actorId)
        );

        return IncidentResponse.from(incident);
    }
}
