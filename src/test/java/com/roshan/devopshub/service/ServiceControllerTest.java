
package com.roshan.devopshub.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ServiceController.class)
class ServiceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ServiceManagementService serviceManagementService;

    @Test
    void createServiceShouldReturn201WhenRequestIsValid()
            throws Exception {

        Instant now = Instant.parse("2026-01-01T10:00:00Z");

        when(serviceManagementService.createService(any()))
                .thenReturn(new ServiceResponse(
                        1L, "Payments", "Payment processing", now, now
                ));

        String request = """
                {
                  "name": "Payments",
                  "description": "Payment processing"
                }
                """;

        mockMvc.perform(post("/api/services")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Payments"))
                .andExpect(jsonPath("$.description")
                        .value("Payment processing"));
    }

    @Test
    void createServiceShouldReturn400WhenNameIsBlank()
            throws Exception {

        String request = """
                {
                  "name": " ",
                  "description": "Payment processing"
                }
                """;

        mockMvc.perform(post("/api/services")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(serviceManagementService);
    }

    @Test
    void getAllServicesShouldReturn200()
            throws Exception {

        Instant now = Instant.parse("2026-01-01T10:00:00Z");

        when(serviceManagementService.getAllServices())
                .thenReturn(List.of(
                        new ServiceResponse(
                                1L, "Payments", "Payment processing", now, now
                        )
                ));

        mockMvc.perform(get("/api/services"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].name").value("Payments"));
    }

    @Test
    void getServiceByIdShouldReturn404WhenMissing() throws Exception {
        when(serviceManagementService.getServiceById(99L))
                .thenThrow(new ServiceNotFoundException(99L));

        mockMvc.perform(get("/api/services/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Service not found"));
    }

    @Test
    void createServiceShouldReturn409WhenNameAlreadyExists()
            throws Exception {

        when(serviceManagementService.createService(any()))
                .thenThrow(new DuplicateServiceNameException());

        String request = """
            {
              "name": "Payments",
              "description": "Payment processing"
            }
            """;

        mockMvc.perform(post("/api/services")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error")
                        .value("Service name already exists"));
    }
}
