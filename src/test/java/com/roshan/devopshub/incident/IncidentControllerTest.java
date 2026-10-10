package com.roshan.devopshub.incident;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import static org.mockito.ArgumentMatchers.eq;
import org.springframework.data.domain.Pageable;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;

import static org.mockito.Mockito.never;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.MediaType;

import java.time.Instant;

@WebMvcTest(IncidentController.class)
class IncidentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private IncidentService incidentService;

    @Test
    void createIncidentShouldNotCallServiceWhenTitleIsBlank()
            throws Exception {

        String request = """
                {
                  "title": "",
                  "description": "Checkout requests are failing.",
                  "severity": "HIGH"
                }
                """;

        mockMvc.perform(post("/api/incidents")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(incidentService);
    }

    @Test
    void createIncidentShouldReturn201WhenRequestIsValid()
            throws Exception {

        Incident incident = new Incident(
                "API gateway returning 500 errors",
                "Checkout requests are failing intermittently.",
                IncidentSeverity.HIGH
        );

        when(incidentService.createIncident(
                any(), any(), any()
        )).thenReturn(incident);

        String request = """
                {
                  "title": "API gateway returning 500 errors",
                  "description": "Checkout requests are failing intermittently.",
                  "severity": "HIGH"
                }
                """;

        mockMvc.perform(post("/api/incidents")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title")
                        .value("API gateway returning 500 errors"))
                .andExpect(jsonPath("$.severity").value("HIGH"))
                .andExpect(jsonPath("$.status").value("OPEN"));
    }

    @Test
    void createIncidentShouldReturn400WhenTitleIsBlank()
            throws Exception {

        String request = """
                {
                  "title": "",
                  "description": "Checkout requests are failing.",
                  "severity": "HIGH"
                }
                """;

        mockMvc.perform(post("/api/incidents")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error")
                        .value("Validation failed"))
                .andExpect(jsonPath("$.details").isArray());
    }

    @Test
    void getIncidentByIdShouldReturn200WhenIncidentExists()
            throws Exception {

        Incident incident = new Incident(
                "API gateway returning 500 errors",
                "Checkout requests are failing intermittently.",
                IncidentSeverity.HIGH
        );

        when(incidentService.getIncidentById(42L))
                .thenReturn(incident);

        mockMvc.perform(get("/api/incidents/42"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title")
                        .value("API gateway returning 500 errors"))
                .andExpect(jsonPath("$.severity").value("HIGH"))
                .andExpect(jsonPath("$.status").value("OPEN"));

        verify(incidentService).getIncidentById(42L);
    }

    @Test
    void getIncidentByIdShouldReturn404WhenIncidentDoesNotExist()
            throws Exception {

        when(incidentService.getIncidentById(999L))
                .thenThrow(new IncidentNotFoundException(999L));

        mockMvc.perform(get("/api/incidents/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error")
                        .value("Incident not found"))
                .andExpect(jsonPath("$.details[0]")
                        .value("Incident not found with id: 999"));

        verify(incidentService).getIncidentById(999L);
    }

    @Test
    void getIncidentsShouldReturnPaginatedResults() throws Exception {

        Incident incident = new Incident(
                "API gateway returning 500 errors",
                "Requests to the checkout endpoint are failing intermittently.",
                IncidentSeverity.HIGH
        );

        Page<Incident> page = new PageImpl<>(
                List.of(incident),
                PageRequest.of(0, 10),
                1
        );

        // No filters were supplied, so both filter arguments are null.
        when(incidentService.getIncidents(
                isNull(),
                isNull(),
                any(PageRequest.class)
        )).thenReturn(page);

        mockMvc.perform(get("/api/incidents?page=0&size=10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].title")
                        .value("API gateway returning 500 errors"))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.totalPages").value(1))
                .andExpect(jsonPath("$.number").value(0));

        verify(incidentService).getIncidents(
                isNull(),
                isNull(),
                any(PageRequest.class)
        );
    }

    @Test
    void getIncidentsShouldReturn400WhenPageIsNegative()
            throws Exception {

        mockMvc.perform(get("/api/incidents?page=-1&size=10"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error")
                        .value("Invalid request"));

        verifyNoInteractions(incidentService);
    }

    @Test
    void getIncidentsShouldReturn400WhenPageSizeExceedsMaximum()
            throws Exception {

        mockMvc.perform(get("/api/incidents?page=0&size=101"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error")
                        .value("Invalid request"));

        verifyNoInteractions(incidentService);
    }

    @Test
    void getIncidentsShouldReturn400WhenPageSizeIsZero()
            throws Exception {

        mockMvc.perform(get("/api/incidents?page=0&size=0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error")
                        .value("Invalid request"));

        verifyNoInteractions(incidentService);
    }

    @Test
    void getIncidentsShouldAcceptMaximumPageSize()
            throws Exception {

        when(incidentService.getIncidents(
                isNull(),
                isNull(),
                any(PageRequest.class)
        )).thenReturn(Page.empty());

        mockMvc.perform(get("/api/incidents?page=0&size=100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content").isEmpty());

        verify(incidentService).getIncidents(
                isNull(),
                isNull(),
                any(PageRequest.class)
        );
    }

    @Test
    void getIncidentsShouldPassFiltersToService() throws Exception {

        when(incidentService.getIncidents(
                eq(IncidentStatus.OPEN),
                eq(IncidentSeverity.HIGH),
                any(Pageable.class)
        )).thenReturn(Page.empty());

        mockMvc.perform(get(
                        "/api/incidents?status=OPEN&severity=HIGH&page=0&size=10"
                ))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isEmpty());

        verify(incidentService).getIncidents(
                eq(IncidentStatus.OPEN),
                eq(IncidentSeverity.HIGH),
                any(Pageable.class)
        );
    }

    @Test
    void transitionIncidentShouldReturn200WhenTransitionIsValid()
            throws Exception {

        Incident incident = new Incident(
                "Payment API failure",
                "Checkout requests are failing.",
                IncidentSeverity.HIGH
        );
        incident.transitionTo(IncidentStatus.IN_PROGRESS);

        when(incidentService.transitionStatus(
                17L, IncidentStatus.IN_PROGRESS
        )).thenReturn(incident);

        String request = """
            {
              "status": "IN_PROGRESS"
            }
            """;

        mockMvc.perform(post("/api/incidents/17/transitions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"));

        verify(incidentService).transitionStatus(
                17L, IncidentStatus.IN_PROGRESS
        );
    }

    @Test
    void transitionIncidentShouldReturn400WhenStatusIsMissing()
            throws Exception {

        String request = "{}";

        mockMvc.perform(post("/api/incidents/17/transitions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(incidentService);
    }


    @Test
    void addIncidentUpdateShouldReturn201WhenRequestIsValid()
            throws Exception {

        Incident incident = new Incident(
                "Payment API failure",
                "Checkout requests are failing.",
                IncidentSeverity.HIGH
        );

        IncidentUpdate update = new IncidentUpdate(
                incident,
                "Database connection pool exhaustion identified."
        );

        when(incidentService.addUpdate(
                eq(17L),
                eq("Database connection pool exhaustion identified.")
        )).thenReturn(update);

        String request = """
            {
              "message": "Database connection pool exhaustion identified."
            }
            """;

        mockMvc.perform(post("/api/incidents/17/updates")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message")
                        .value("Database connection pool exhaustion identified."));

        verify(incidentService).addUpdate(
                17L,
                "Database connection pool exhaustion identified."
        );
    }


    @Test
    void addIncidentUpdateShouldReturn400WhenMessageIsBlank()
            throws Exception {

        String request = """
            {
              "message": "   "
            }
            """;

        mockMvc.perform(post("/api/incidents/17/updates")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(incidentService);
    }

    @Test
    void addIncidentUpdateShouldReturn404WhenIncidentDoesNotExist()
            throws Exception {

        when(incidentService.addUpdate(
                eq(999L),
                eq("Investigation update")
        )).thenThrow(new IncidentNotFoundException(999L));

        String request = """
            {
              "message": "Investigation update"
            }
            """;

        mockMvc.perform(post("/api/incidents/999/updates")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error")
                        .value("Incident not found"));

        verify(incidentService).addUpdate(999L, "Investigation update");
    }

    @Test
    void getIncidentUpdatesShouldReturn200WhenUpdatesExist()
            throws Exception {

        Incident incident = new Incident(
                "Payment API failure",
                "Checkout requests are failing.",
                IncidentSeverity.HIGH
        );

        IncidentUpdate update = new IncidentUpdate(
                incident,
                "Database connection pool exhaustion identified."
        );

        when(incidentService.getIncidentUpdates(17L))
                .thenReturn(List.of(update));

        mockMvc.perform(get("/api/incidents/17/updates"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].message")
                        .value("Database connection pool exhaustion identified."));

        verify(incidentService).getIncidentUpdates(17L);
    }

    @Test
    void getIncidentUpdatesShouldReturnEmptyArrayWhenNoUpdatesExist()
            throws Exception {

        when(incidentService.getIncidentUpdates(17L))
                .thenReturn(List.of());

        mockMvc.perform(get("/api/incidents/17/updates"))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));

        verify(incidentService).getIncidentUpdates(17L);
    }

    @Test
    void getIncidentUpdatesShouldReturn404WhenIncidentDoesNotExist()
            throws Exception {

        when(incidentService.getIncidentUpdates(999L))
                .thenThrow(new IncidentNotFoundException(999L));

        mockMvc.perform(get("/api/incidents/999/updates"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error")
                        .value("Incident not found"));

        verify(incidentService).getIncidentUpdates(999L);
    }

    @Test
    void getIncidentHistoryShouldReturnChronologicalHistory() throws Exception {
        Instant earlier = Instant.parse("2026-10-11T10:00:00Z");
        Instant later = Instant.parse("2026-10-11T10:05:00Z");

        List<IncidentHistoryItemResponse> history = List.of(
                new IncidentHistoryItemResponse(
                        "STATUS_CHANGED",
                        "OPEN -> IN_PROGRESS",
                        earlier
                ),
                new IncidentHistoryItemResponse(
                        "INVESTIGATION_UPDATE",
                        "Investigating database connections.",
                        later
                )
        );

        when(incidentService.getIncidentHistory(17L))
                .thenReturn(history);

        mockMvc.perform(get("/api/incidents/17/history"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].type").value("STATUS_CHANGED"))
                .andExpect(jsonPath("$[0].details")
                        .value("OPEN -> IN_PROGRESS"))
                .andExpect(jsonPath("$[1].type")
                        .value("INVESTIGATION_UPDATE"))
                .andExpect(jsonPath("$[1].details")
                        .value("Investigating database connections."));

        verify(incidentService).getIncidentHistory(17L);
    }

    @Test
    void getIncidentHistoryShouldReturn404WhenIncidentDoesNotExist()
            throws Exception {

        when(incidentService.getIncidentHistory(999L))
                .thenThrow(new IncidentNotFoundException(999L));

        mockMvc.perform(get("/api/incidents/999/history"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error")
                        .value("Incident not found"));

        verify(incidentService).getIncidentHistory(999L);
    }


}