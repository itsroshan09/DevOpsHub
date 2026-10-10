
package com.roshan.devopshub.incident;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.verify;
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

    @Mock
    private IncidentUpdateRepository incidentUpdateRepository;

    @BeforeEach
    void setUp() {
        incidentService = new IncidentService(
                incidentRepository,
                incidentUpdateRepository
        );
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

    @Test
    void getIncidentsShouldRequestPageFromRepository() {
        PageRequest pageable = PageRequest.of(0, 10);

        Page<Incident> expectedPage = new PageImpl<>(
                List.of(new Incident(
                        "API gateway returning 500 errors",
                        "Checkout requests are failing intermittently.",
                        IncidentSeverity.HIGH
                )),
                pageable,
                1
        );

        when(incidentRepository.findAll(pageable))
                .thenReturn(expectedPage);

        Page<Incident> result = incidentService.getIncidents(pageable);

        assertSame(expectedPage, result);
        verify(incidentRepository).findAll(pageable);
    }


    @Test
    void transitionStatusShouldAllowOpenToInProgress() {
        Incident incident = new Incident(
                "Payment API failure",
                "Checkout requests are failing.",
                IncidentSeverity.HIGH
        );

        when(incidentRepository.findById(17L))
                .thenReturn(Optional.of(incident));
        when(incidentRepository.save(incident))
                .thenReturn(incident);

        Incident result = incidentService.transitionStatus(
                17L, IncidentStatus.IN_PROGRESS
        );

        assertEquals(IncidentStatus.IN_PROGRESS, result.getStatus());
        verify(incidentRepository).save(incident);
    }

    @Test
    void transitionStatusShouldAllowInProgressToResolved() {
        Incident incident = new Incident(
                "Payment API failure",
                "Checkout requests are failing.",
                IncidentSeverity.HIGH
        );
        incident.transitionTo(IncidentStatus.IN_PROGRESS);

        when(incidentRepository.findById(17L))
                .thenReturn(Optional.of(incident));
        when(incidentRepository.save(incident))
                .thenReturn(incident);

        Incident result = incidentService.transitionStatus(
                17L, IncidentStatus.RESOLVED
        );

        assertEquals(IncidentStatus.RESOLVED, result.getStatus());
        verify(incidentRepository).save(incident);
    }

    @Test
    void transitionStatusShouldRejectOpenToResolved() {
        Incident incident = new Incident(
                "Payment API failure",
                "Checkout requests are failing.",
                IncidentSeverity.HIGH
        );

        when(incidentRepository.findById(17L))
                .thenReturn(Optional.of(incident));

        assertThrows(
                IllegalArgumentException.class,
                () -> incidentService.transitionStatus(
                        17L, IncidentStatus.RESOLVED
                )
        );

        assertEquals(IncidentStatus.OPEN, incident.getStatus());
        verify(incidentRepository, never()).save(any(Incident.class));
    }

    @Test
    void transitionStatusShouldThrowWhenIncidentDoesNotExist() {
        when(incidentRepository.findById(999L))
                .thenReturn(Optional.empty());

        assertThrows(
                IncidentNotFoundException.class,
                () -> incidentService.transitionStatus(
                        999L, IncidentStatus.IN_PROGRESS
                )
        );

        verify(incidentRepository, never()).save(any(Incident.class));
    }


    @Test
    void addUpdateShouldSaveAndReturnUpdate() {
        Incident incident = new Incident(
                "Payment API failure",
                "Checkout requests are failing.",
                IncidentSeverity.HIGH
        );

        IncidentUpdate update = new IncidentUpdate(
                incident,
                "Database connection pool exhaustion identified."
        );

        when(incidentRepository.findById(17L))
                .thenReturn(Optional.of(incident));

        when(incidentUpdateRepository.save(any(IncidentUpdate.class)))
                .thenReturn(update);

        IncidentUpdate result = incidentService.addUpdate(
                17L,
                "Database connection pool exhaustion identified."
        );

        assertNotNull(result);
        assertSame(update, result);
        assertEquals(incident, result.getIncident());
        assertEquals(
                "Database connection pool exhaustion identified.",
                result.getMessage()
        );

        verify(incidentRepository).findById(17L);
        verify(incidentUpdateRepository).save(any(IncidentUpdate.class));
    }


    @Test
    void addUpdateShouldThrowWhenIncidentDoesNotExist() {
        when(incidentRepository.findById(999L))
                .thenReturn(Optional.empty());

        assertThrows(
                IncidentNotFoundException.class,
                () -> incidentService.addUpdate(
                        999L,
                        "Investigation update"
                )
        );

        verify(incidentRepository).findById(999L);
        verify(incidentUpdateRepository, never())
                .save(any(IncidentUpdate.class));
    }


    @Test
    void getIncidentUpdatesShouldReturnUpdatesInRepositoryOrder() {
        Incident incident = new Incident(
                "Payment API failure",
                "Checkout requests are failing.",
                IncidentSeverity.HIGH
        );

        IncidentUpdate firstUpdate = new IncidentUpdate(
                incident,
                "Investigating database connections."
        );

        IncidentUpdate secondUpdate = new IncidentUpdate(
                incident,
                "Connection pool exhaustion confirmed."
        );

        List<IncidentUpdate> expectedUpdates =
                List.of(firstUpdate, secondUpdate);

        when(incidentRepository.findById(17L))
                .thenReturn(Optional.of(incident));

        when(incidentUpdateRepository
                .findByIncidentIdOrderByCreatedAtAsc(17L))
                .thenReturn(expectedUpdates);

        List<IncidentUpdate> result =
                incidentService.getIncidentUpdates(17L);

        assertEquals(2, result.size());
        assertSame(firstUpdate, result.get(0));
        assertSame(secondUpdate, result.get(1));

        verify(incidentRepository).findById(17L);
        verify(incidentUpdateRepository)
                .findByIncidentIdOrderByCreatedAtAsc(17L);
    }

    @Test
    void getIncidentUpdatesShouldThrowWhenIncidentDoesNotExist() {
        when(incidentRepository.findById(999L))
                .thenReturn(Optional.empty());

        assertThrows(
                IncidentNotFoundException.class,
                () -> incidentService.getIncidentUpdates(999L)
        );

        verify(incidentRepository).findById(999L);

        verify(incidentUpdateRepository, never())
                .findByIncidentIdOrderByCreatedAtAsc(999L);
    }


}
