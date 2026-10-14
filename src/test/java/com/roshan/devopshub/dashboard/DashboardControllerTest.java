package com.roshan.devopshub.dashboard;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class DashboardControllerTest {

    private DashboardService dashboardService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        dashboardService = mock(DashboardService.class);

        DashboardController controller =
                new DashboardController(dashboardService);

        mockMvc = MockMvcBuilders
                .standaloneSetup(controller)
                .build();
    }

    @Test
    void shouldReturnDashboardSummaryAsJson() throws Exception {
        when(dashboardService.getSummary()).thenReturn(
                new DashboardSummaryResponse(
                        10L, 2L, 3L, 5L,
                        1L, 2L, 4L, 3L
                )
        );

        mockMvc.perform(get("/api/dashboard/summary")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalIncidents").value(10))
                .andExpect(jsonPath("$.openIncidents").value(2))
                .andExpect(jsonPath("$.inProgressIncidents").value(3))
                .andExpect(jsonPath("$.resolvedIncidents").value(5))
                .andExpect(jsonPath("$.lowSeverityIncidents").value(1))
                .andExpect(jsonPath("$.mediumSeverityIncidents").value(2))
                .andExpect(jsonPath("$.highSeverityIncidents").value(4))
                .andExpect(jsonPath("$.criticalSeverityIncidents").value(3));

        verify(dashboardService).getSummary();
    }
}
