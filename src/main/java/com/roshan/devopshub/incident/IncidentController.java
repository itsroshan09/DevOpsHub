
package com.roshan.devopshub.incident;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

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
}
