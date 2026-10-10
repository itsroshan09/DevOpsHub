package com.roshan.devopshub.incident;

import static org.mockito.Mockito.verifyNoInteractions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;
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

        when(incidentService.getIncidents(any()))
                .thenReturn(page);

        mockMvc.perform(get("/api/incidents?page=0&size=10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].title")
                        .value("API gateway returning 500 errors"))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.totalPages").value(1))
                .andExpect(jsonPath("$.number").value(0));
    }


    @Test
    void getIncidentsShouldReturn400WhenPageIsNegative()
            throws Exception {

        mockMvc.perform(get("/api/incidents?page=-1&size=10"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Invalid request"));
    }

    @Test
    void getIncidentsShouldReturn400WhenPageSizeExceedsMaximum()
            throws Exception {

        mockMvc.perform(get("/api/incidents?page=0&size=101"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Invalid request"));
    }

    @Test
    void getIncidentsShouldReturn400WhenPageSizeIsZero()
            throws Exception {

        mockMvc.perform(get("/api/incidents?page=0&size=0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Invalid request"));
    }

    @Test
    void getIncidentsShouldAcceptMaximumPageSize()
            throws Exception {

        when(incidentService.getIncidents(any()))
                .thenReturn(Page.empty());

        mockMvc.perform(get("/api/incidents?page=0&size=100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content").isEmpty());
    }

}
