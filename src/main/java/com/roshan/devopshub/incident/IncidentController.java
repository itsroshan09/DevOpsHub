
package com.roshan.devopshub.incident;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;

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
            @PageableDefault(size = 10, sort = "createdAt",
                    direction = org.springframework.data.domain.Sort.Direction.DESC)
            Pageable pageable) {

        return incidentService.getIncidents(pageable)
                .map(IncidentResponse::from);
    }

}
