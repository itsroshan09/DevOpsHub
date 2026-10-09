
package com.roshan.devopshub.incident;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import java.util.Optional;

@ExtendWith(MockitoExtension.class)
class IncidentServiceTest {

    @Mock
    private IncidentRepository incidentRepository;

    private IncidentService incidentService;

    @BeforeEach
    void setUp() {
        incidentService = new IncidentService(incidentRepository);
    }

    @Test
    void createIncidentShouldSaveAndReturnIncident() {
        Incident incident = new Incident(
                "API gateway returning 500 errors",
                "Checkout requests are failing intermittently.",
                IncidentSeverity.HIGH
        );

        when(incidentRepository.save(any(Incident.class)))
                .thenReturn(incident);

        Incident result = incidentService.createIncident(
                "API gateway returning 500 errors",
                "Checkout requests are failing intermittently.",
                IncidentSeverity.HIGH
        );

        assertNotNull(result);
        assertEquals("API gateway returning 500 errors", result.getTitle());
        assertEquals(IncidentSeverity.HIGH, result.getSeverity());
        assertEquals(IncidentStatus.OPEN, result.getStatus());

        verify(incidentRepository).save(any(Incident.class));
    }

    @Test
    void getIncidentByIdShouldThrowExceptionWhenIncidentDoesNotExist() {
        when(incidentRepository.findById(999L))
                .thenReturn(java.util.Optional.empty());

        assertThrows(
                IncidentNotFoundException.class,
                () -> incidentService.getIncidentById(999L)
        );

        verify(incidentRepository).findById(999L);
    }




}
