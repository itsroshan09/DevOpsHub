package com.roshan.devopshub.dashboard;

import com.roshan.devopshub.incident.IncidentRepository;
import com.roshan.devopshub.incident.IncidentSeverity;
import com.roshan.devopshub.incident.IncidentStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    @Mock
    private IncidentRepository incidentRepository;

    @InjectMocks
    private DashboardService dashboardService;

    @Test
    void shouldReturnSummaryFromRepositoryCounts() {
        when(incidentRepository.countIncidentsByStatus())
                .thenReturn(List.of(
                        new Object[]{IncidentStatus.OPEN, 4L},
                        new Object[]{IncidentStatus.IN_PROGRESS, 3L},
                        new Object[]{IncidentStatus.RESOLVED, 2L}
                ));

        when(incidentRepository.countIncidentsBySeverity())
                .thenReturn(List.of(
                        new Object[]{IncidentSeverity.LOW, 1L},
                        new Object[]{IncidentSeverity.MEDIUM, 2L},
                        new Object[]{IncidentSeverity.HIGH, 4L},
                        new Object[]{IncidentSeverity.CRITICAL, 2L}
                ));

        DashboardSummaryResponse response =
                dashboardService.getSummary();

        assertEquals(9L, response.totalIncidents());
        assertEquals(4L, response.openIncidents());
        assertEquals(3L, response.inProgressIncidents());
        assertEquals(2L, response.resolvedIncidents());
        assertEquals(1L, response.lowSeverityIncidents());
        assertEquals(2L, response.mediumSeverityIncidents());
        assertEquals(4L, response.highSeverityIncidents());
        assertEquals(2L, response.criticalSeverityIncidents());

        verify(incidentRepository).countIncidentsByStatus();
        verify(incidentRepository).countIncidentsBySeverity();
    }

    @Test
    void shouldReturnZeroCountsWhenThereAreNoIncidents() {
        when(incidentRepository.countIncidentsByStatus())
                .thenReturn(List.of());

        when(incidentRepository.countIncidentsBySeverity())
                .thenReturn(List.of());

        DashboardSummaryResponse response =
                dashboardService.getSummary();

        assertEquals(0L, response.totalIncidents());
        assertEquals(0L, response.openIncidents());
        assertEquals(0L, response.inProgressIncidents());
        assertEquals(0L, response.resolvedIncidents());
        assertEquals(0L, response.lowSeverityIncidents());
        assertEquals(0L, response.mediumSeverityIncidents());
        assertEquals(0L, response.highSeverityIncidents());
        assertEquals(0L, response.criticalSeverityIncidents());
    }

    @Test
    void shouldTreatMissingCategoriesAsZero() {
        when(incidentRepository.countIncidentsByStatus())
                .thenReturn(List.<Object[]>of(
                        new Object[]{IncidentStatus.OPEN, 2L}
                ));

        when(incidentRepository.countIncidentsBySeverity())
                .thenReturn(List.<Object[]>of(
                        new Object[]{IncidentSeverity.HIGH, 2L}
                ));

        DashboardSummaryResponse response =
                dashboardService.getSummary();

        assertEquals(2L, response.totalIncidents());
        assertEquals(2L, response.openIncidents());
        assertEquals(0L, response.inProgressIncidents());
        assertEquals(0L, response.resolvedIncidents());
        assertEquals(0L, response.lowSeverityIncidents());
        assertEquals(0L, response.mediumSeverityIncidents());
        assertEquals(2L, response.highSeverityIncidents());
        assertEquals(0L, response.criticalSeverityIncidents());
    }
}
